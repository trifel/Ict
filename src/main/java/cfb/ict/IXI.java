package cfb.ict;

import java.util.LinkedList;
import java.util.List;
import cfb.ict.service.CallableRequest;
import cfb.ict.service.dto.AbstractResponse;
import cfb.ict.service.dto.ErrorResponse;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.nio.file.SensitivityWatchEventModifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.sun.jmx.mbeanserver.Util.cast;
import static java.nio.file.StandardWatchEventKinds.*;

public class IXI {

    private static final Logger log = LoggerFactory.getLogger(IXI.class);
    private static final int MAX_TREE_DEPTH = 2;
    private static final String IXI_DIRECTORY = "ixi";

    private final Gson gson = new GsonBuilder().create();
    private final ScriptEngine scriptEngine = (new ScriptEngineManager()).getEngineByName("JavaScript");
    private final Map<String, Map<String, CallableRequest<AbstractResponse>>> ixiAPI = new HashMap<>();
    private final Map<String, Map<String, Runnable>> ixiLifetime = new HashMap<>();
    private final Map<WatchKey, Path> watchKeys = new HashMap<>();
    private final Map<Path, Long> loadedLastTime = new HashMap<>();

    private WatchService watcher;
    private Thread dirWatchThread;
    private Path rootPath;

    private boolean shutdown = false;

    private final Node node;
    private final Tangle tangle;

    IXI(final Tangle tangle, final Node node) {

        this.tangle = tangle;
        this.node = node;
    }

    List<Transaction> getTransactions(final List<Hash> hashes) {

        final List<Transaction> result = new LinkedList<>();

        for (final Hash hash : hashes) {

            final Tangle.Vertex vertex = tangle.get(hash);
            if (vertex != null && vertex.transaction != null) {

                result.add(vertex.transaction);
            }
        }

        return result;
    }

    List<Hash> putTransactions(final List<Transaction> transactions) {

        final List<Hash> result = new LinkedList<>();

        for (final Transaction transaction : transactions) {

            if (tangle.put(transaction, null)) {

                node.replicate(transaction);

                result.add(transaction.hash);
            }
        }

        return result;
    }

    List<Hash> removeTransactions(final List<Hash> hashes) {

        final List<Hash> result = new LinkedList<>();

        for (final Hash hash : hashes) {

            if (tangle.remove(hash)) {

                result.add(hash);
            }
        }

        return result;
    }

    public void init() throws Exception {
        watcher = FileSystems.getDefault().newWatchService();
        this.rootPath = Paths.get(IXI_DIRECTORY);
        if(this.rootPath.toFile().exists() || this.rootPath.toFile().mkdir()) {
            registerRecursive(this.rootPath);
            dirWatchThread = (new Thread(this::processWatchEvents));
            dirWatchThread.start();
        }
    }

    private void registerRecursive(final Path root) throws IOException {
        Files.walkFileTree(root, EnumSet.allOf(FileVisitOption.class), MAX_TREE_DEPTH, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path modulePath, BasicFileAttributes attrs) throws IOException {
                watch(modulePath);
                if (modulePath != rootPath) {
                    loadModule(modulePath);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void unregisterRecursive(final Path root) throws IOException {
        Files.walkFileTree(root, EnumSet.allOf(FileVisitOption.class), MAX_TREE_DEPTH, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path modulePath, BasicFileAttributes attrs) throws IOException {
                if (modulePath != rootPath) {
                    unloadModule(modulePath);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void processWatchEvents() {
        while(!shutdown) {
            WatchKey key = null;
            try {
                key = watcher.poll(1000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                log.error("Watcher interrupted: ", e);
            }
            if (key == null) {
                continue;
            }
            WatchKey finalKey = key;
            key.pollEvents().forEach(watchEvent -> {
                WatchEvent<Path> pathEvent = cast(watchEvent);
                IxiEvent ixiEvent = IxiEvent.fromName(watchEvent.kind().name());
                Path watchedPath = watchKeys.get(finalKey);
                if (watchedPath != null) {
                    handleModulePathEvent(watchedPath, ixiEvent, watchedPath.resolve(pathEvent.context()));
                }
            });
            key.reset();
        }
    }

    private String getModuleName(Path modulePath, boolean checkIfIsDir) {
        return rootPath.relativize(!checkIfIsDir || Files.isDirectory(modulePath) ? modulePath : modulePath.getParent()).toString();
    }

    private Path getRealPath(Path currentPath) {
        if (Files.isDirectory(currentPath.getParent()) && !currentPath.getParent().equals(rootPath)) {
            return currentPath.getParent();
        } else {
            return currentPath;
        }
    }

    private void handleModulePathEvent(Path watchedPath, IxiEvent ixiEvent, Path changedPath) {
        if (watchedPath != rootPath && Files.isDirectory(changedPath)) { // we are only interested in dir changes in tree depth level 2
            return;
        }
        handlePathEvent(ixiEvent, changedPath);
    }

    private void handlePathEvent(IxiEvent ixiEvent, Path changedPath) {
        switch(ixiEvent) {
            case CREATE_MODULE:
                if (checkOs() == OsVariants.Unix) {
                    watch(changedPath);
                    loadModule(changedPath);
                }
                break;
            case MODIFY_MODULE:
                Long lastModification = loadedLastTime.get(getRealPath(changedPath));
                if (lastModification == null || Instant.now().toEpochMilli() - lastModification > 50L) {
                    if (ixiLifetime.containsKey(getModuleName(changedPath, true))) {
                        unloadModule(changedPath);
                    }
                    loadedLastTime.put(getRealPath(changedPath), Instant.now().toEpochMilli());
                    loadModule(getRealPath(changedPath));
                }
                break;
            case DELETE_MODULE:
                Path realPath = getRealPath(changedPath);
                unwatch(realPath);
                if (ixiLifetime.containsKey(getModuleName(realPath, false))) {
                    unloadModule(changedPath);
                }
                break;
            default:
        }
    }

    private static OsVariants checkOs() {
        String os = System.getProperty("os.name");
        if (os.startsWith("Windows")) {
            return OsVariants.Windows;
        } else {
            return OsVariants.Unix;
        }
    }

    private void watch(Path dir) {
        try {
            WatchKey watchKey = dir.register(watcher, new WatchEvent.Kind[]{ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY}, SensitivityWatchEventModifier.HIGH);
            watchKeys.put(watchKey, dir);
        } catch (IOException e) {
            log.error("Could not create watcher for path '" + dir + "'.");
        }
    }

    private void unwatch(Path dir) {
        // TODO: Get watchkey for 'dir' in an optimized way
        Optional<WatchKey> dirKey = watchKeys.keySet().stream().filter(watchKey -> watchKeys.get(watchKey).equals(dir)).findFirst();
        if (dirKey.isPresent()) {
            watchKeys.remove(dirKey.get());
            dirKey.get().cancel();
        }
    }

    private Path getPackagePath(Path modulePath) {
        return modulePath.resolve("package.json");
    }

    public AbstractResponse processCommand(final String command, Map<String, Object> request) {
        if(command == null || command.isEmpty()) {
            return ErrorResponse.create("Command can not be null or empty");
        }

        Pattern pattern = Pattern.compile("^(.*)\\.(.*)$");
        Matcher matcher = pattern.matcher(command);

        if (matcher.find()) {
            Map<String, CallableRequest<AbstractResponse>> ixiMap = ixiAPI.get(matcher.group(1));
            if (ixiMap != null) {
                return ixiMap.get(matcher.group(2)).call(request);
            }
        }
        return ErrorResponse.create("Command [" + command + "] is unknown");
    }

    private void loadModule(Path modulePath) {
        log.info("Searching: " + modulePath);
        Path packageJsonPath = getPackagePath(modulePath);
        if (!Files.exists(packageJsonPath)) {
            log.info("No package.json found in " + modulePath);
            return;
        }
        final Map packageJson;
        Reader packageJsonReader;
        try {
            packageJsonReader = new FileReader(packageJsonPath.toFile());
            packageJson = gson.fromJson(packageJsonReader, Map.class);
        } catch (FileNotFoundException e) {
            log.error("Could not load " + packageJsonPath.toString());
            return;
        }
        try {
            packageJsonReader.close();
        } catch (IOException e) {
            log.error("Could not close file " + packageJsonPath.toString());
        }
        if(packageJson != null && packageJson.get("main") != null) {
            log.info("Loading module: " + getModuleName(modulePath, true));
            Path pathToMain = Paths.get(modulePath.toString(), (String) packageJson.get("main"));
            attach(pathToMain, getModuleName(modulePath, true));
        } else {
            log.info("No start script found");
        }
    }

    private void unloadModule(Path moduleNamePath) {
        log.debug("Unloading module: " + moduleNamePath);
        Path realPath = getRealPath(moduleNamePath);
        String moduleName = getModuleName(realPath, false);
        detach(moduleName);
        ixiAPI.remove(moduleName);
    }

    private void attach(Path pathToMain, String moduleName) {
        Reader ixiModuleReader;
        try {
            ixiModuleReader = new FileReader(pathToMain.toFile());
        } catch (FileNotFoundException e) {
            log.error("Could not load " + pathToMain);
            return;
        }
        log.info("Starting script: " + pathToMain);
        Map<String, CallableRequest<AbstractResponse>> ixiMap = new HashMap<>();
        Map<String, Runnable> startStop = new HashMap<>();

        Bindings bindings = scriptEngine.createBindings();
        bindings.put("API", ixiMap);
        bindings.put("IXICycle", startStop);
        bindings.put("NODE", node);
        bindings.put("VERSION", Ict.VERSION);

        ixiAPI.put(moduleName, ixiMap);
        ixiLifetime.put(moduleName, startStop);
        try {
            scriptEngine.eval(ixiModuleReader, bindings);
        } catch (ScriptException e) {
            e.printStackTrace();
            log.error("Script error");
        }
        try {
            ixiModuleReader.close();
        } catch (IOException e) {
            log.error("Could not close " + pathToMain);
        }
    }

    private void detach(String moduleName) {
        Map<String, Runnable> ixiMap = ixiLifetime.get(moduleName);
        if(ixiMap != null) {
            Runnable stop = ixiMap.get("shutdown");
            if (stop != null) {
                stop.run();
            }
        }
        ixiLifetime.remove(moduleName);
    }

    public void shutdown() throws InterruptedException, IOException {
        unregisterRecursive(this.rootPath);       
        if(dirWatchThread != null) {
            shutdown = true;
            dirWatchThread.join();
            ixiAPI.keySet().forEach(this::detach);
            ixiAPI.clear();
            ixiLifetime.clear();
        }
    }
}
