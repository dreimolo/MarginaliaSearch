package nu.marginalia.actor;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.SneakyThrows;
import nu.marginalia.actor.monitor.*;
import nu.marginalia.actor.proc.*;
import nu.marginalia.actor.prototype.ActorPrototype;
import nu.marginalia.actor.prototype.RecordActorPrototype;
import nu.marginalia.actor.state.ActorStateInstance;
import nu.marginalia.actor.state.ActorStep;
import nu.marginalia.actor.task.*;
import nu.marginalia.mq.MessageQueueFactory;
import nu.marginalia.service.control.ServiceEventLog;
import nu.marginalia.service.server.BaseServiceParams;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** This class is responsible for starting and stopping the various actors in the responsible service */
@Singleton
public class ExecutorActorControlService {
    private final ServiceEventLog eventLog;
    private final MessageQueueFactory messageQueueFactory;
    private final ExecutorActorStateMachines stateMachines;
    public Map<ExecutorActor, ActorPrototype> actorDefinitions = new HashMap<>();
    private final int node;
    @Inject
    public ExecutorActorControlService(MessageQueueFactory messageQueueFactory,
                                       BaseServiceParams baseServiceParams,
                                       ConvertActor convertActor,
                                       ConvertAndLoadActor convertAndLoadActor,
                                       CrawlActor crawlActor,
                                       RecrawlActor recrawlActor,
                                       RestoreBackupActor restoreBackupActor,
                                       ConverterMonitorActor converterMonitorFSM,
                                       CrawlerMonitorActor crawlerMonitorActor,
                                       LoaderMonitorActor loaderMonitor,
                                       ProcessLivenessMonitorActor processMonitorFSM,
                                       FileStorageMonitorActor fileStorageMonitorActor,
                                       IndexConstructorMonitorActor indexConstructorMonitorActor,
                                       TriggerAdjacencyCalculationActor triggerAdjacencyCalculationActor,
                                       CrawlJobExtractorActor crawlJobExtractorActor,
                                       ExportDataActor exportDataActor,
                                       ExportAtagsActor exportAtagsActor,
                                       ExecutorActorStateMachines stateMachines) {
        this.messageQueueFactory = messageQueueFactory;
        this.eventLog = baseServiceParams.eventLog;
        this.stateMachines = stateMachines;
        this.node = baseServiceParams.configuration.node();

        register(ExecutorActor.CRAWL, crawlActor);
        register(ExecutorActor.RECRAWL, recrawlActor);
        register(ExecutorActor.CONVERT, convertActor);
        register(ExecutorActor.RESTORE_BACKUP, restoreBackupActor);
        register(ExecutorActor.CONVERT_AND_LOAD, convertAndLoadActor);

        register(ExecutorActor.PROC_INDEX_CONSTRUCTOR_SPAWNER, indexConstructorMonitorActor);
        register(ExecutorActor.PROC_CONVERTER_SPAWNER, converterMonitorFSM);
        register(ExecutorActor.PROC_LOADER_SPAWNER, loaderMonitor);
        register(ExecutorActor.PROC_CRAWLER_SPAWNER, crawlerMonitorActor);

        register(ExecutorActor.MONITOR_PROCESS_LIVENESS, processMonitorFSM);
        register(ExecutorActor.MONITOR_FILE_STORAGE, fileStorageMonitorActor);

        register(ExecutorActor.ADJACENCY_CALCULATION, triggerAdjacencyCalculationActor);
        register(ExecutorActor.CRAWL_JOB_EXTRACTOR, crawlJobExtractorActor);
        register(ExecutorActor.EXPORT_DATA, exportDataActor);
        register(ExecutorActor.EXPORT_ATAGS, exportAtagsActor);
    }

    private void register(ExecutorActor process, RecordActorPrototype graph) {
        var sm = new ActorStateMachine(messageQueueFactory, process.id(), node, UUID.randomUUID(), graph);
        sm.listen((function, param) -> logStateChange(process, function));

        stateMachines.put(process, sm);
        actorDefinitions.put(process, graph);
    }

    private void logStateChange(ExecutorActor process, String state) {
        eventLog.logEvent("FSM-STATE-CHANGE", process.id() + " -> " + state);
    }

    public void start(ExecutorActor process) throws Exception {
        eventLog.logEvent("FSM-START", process.id());

        stateMachines.init(process);
    }

    public <T> void startFrom(ExecutorActor process, ActorStep step) throws Exception {
        eventLog.logEvent("FSM-START", process.id());

        stateMachines.initFrom(process, step);
    }

    public <T> void startFromJSON(ExecutorActor process, String state, String json) throws Exception {
        eventLog.logEvent("FSM-START", process.id());

        stateMachines.startFromJSON(process, state, json);
    }

    @SneakyThrows
    public void stop(ExecutorActor process) {
        eventLog.logEvent("FSM-STOP", process.id());

        stateMachines.stop(process);
    }

    public Map<ExecutorActor, ActorStateInstance> getActorStates() {
        return stateMachines.getActorStates();
    }

    public boolean isDirectlyInitializable(ExecutorActor actor) {
        return actorDefinitions.get(actor).isDirectlyInitializable();
    }

    public ActorPrototype getActorDefinition(ExecutorActor actor) {
        return actorDefinitions.get(actor);
    }

}
