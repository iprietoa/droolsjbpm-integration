package org.jbpm.simulation.helper;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.drools.impl.EnvironmentFactory;
import org.eclipse.bpmn2.FlowElement;
import org.jbpm.simulation.PathContext;
import org.jbpm.simulation.impl.SimulationEndNodeInstance;
import org.jbpm.simulation.impl.SimulationNodeInstance;
import org.jbpm.simulation.impl.SimulationStartNodeInstance;
import org.jbpm.workflow.core.node.ActionNode;
import org.jbpm.workflow.core.node.CatchLinkNode;
import org.jbpm.workflow.core.node.CompositeContextNode;
import org.jbpm.workflow.core.node.CompositeNode;
import org.jbpm.workflow.core.node.DynamicNode;
import org.jbpm.workflow.core.node.EndNode;
import org.jbpm.workflow.core.node.EventNode;
import org.jbpm.workflow.core.node.FaultNode;
import org.jbpm.workflow.core.node.ForEachNode;
import org.jbpm.workflow.core.node.HumanTaskNode;
import org.jbpm.workflow.core.node.Join;
import org.jbpm.workflow.core.node.MilestoneNode;
import org.jbpm.workflow.core.node.RuleSetNode;
import org.jbpm.workflow.core.node.Split;
import org.jbpm.workflow.core.node.StartNode;
import org.jbpm.workflow.core.node.StateNode;
import org.jbpm.workflow.core.node.SubProcessNode;
import org.jbpm.workflow.core.node.ThrowLinkNode;
import org.jbpm.workflow.core.node.TimerNode;
import org.jbpm.workflow.core.node.WorkItemNode;
import org.jbpm.workflow.instance.impl.NodeInstanceFactoryRegistry;
import org.jbpm.workflow.instance.impl.factory.CreateNewNodeFactory;
import org.jbpm.workflow.instance.impl.factory.ReuseNodeFactory;
import org.json.JSONObject;
import org.kie.KieServices;
import org.kie.KnowledgeBase;
import org.kie.KnowledgeBaseFactory;
import org.kie.builder.KieBuilder;
import org.kie.builder.KieFileSystem;
import org.kie.builder.KieModule;
import org.kie.builder.KnowledgeBuilder;
import org.kie.builder.KnowledgeBuilderFactory;
import org.kie.builder.ReleaseId;
import org.kie.builder.model.KieBaseModel;
import org.kie.builder.model.KieModuleModel;
import org.kie.builder.model.KieSessionModel;
import org.kie.conf.EqualityBehaviorOption;
import org.kie.conf.EventProcessingOption;
import org.kie.io.Resource;
import org.kie.io.ResourceFactory;
import org.kie.io.ResourceType;
import org.kie.runtime.KieSessionConfiguration;
import org.kie.runtime.StatefulKnowledgeSession;
import org.kie.runtime.conf.ClockTypeOption;

public class TestUtils {

    public static boolean matchExpected(List<PathContext> paths, List<String>... expectedIds) {
        
        for (PathContext context : paths) {
            List<FlowElement> elements = removeDuplicates(context.getPathElements());
            boolean match = false;
            for (int i = 0; i < expectedIds.length; i++) {
                List<String> expected = expectedIds[i];
                
                if (expected != null && elements.size() == expected.size()) {
                    
                    for (FlowElement fe : elements) {
                        if (!expected.contains(fe.getId())) {
                            System.err.println("Following element not matched: " + fe.getId() + " " + fe.getName());
                            match = false;
                            break;
                        } 
                        match = true;
                    }
                    if (match) {
                        expectedIds[i] = null;
                        break;
                    }
                }
            }
            
            if (!match) {
                return false;
            }
        }
        
        return true;
    }
    
    public static void printOutPaths(List<PathContext> paths, String name) {
        if (!"true".equalsIgnoreCase(System.getProperty("test.debug.off"))) {
            System.out.println("###################" + name + "###################");
            for (PathContext context : paths) {
                System.out.println("PATH: " + context.getId());
                System.out.println("AS TEXT:");
                for (FlowElement fe : context.getPathElements()) {
                    System.out.println(fe.getName() + "  - " + fe.eClass().getName());
                }
            }
            System.out.println("#####################################################");
        }
    }
    
    public static void printOutPaths(List<PathContext> paths, JSONObject jsonPaths, String name) {
        if (!"true".equalsIgnoreCase(System.getProperty("test.debug.off"))) {
            System.out.println("###################" + name + "###################");
            for (PathContext context : paths) {
                System.out.println("$$$$$$$$ PATH: " + context.getId() + " " + context.getType());
                System.out.println("$$$ AS TEXT:");
                for (FlowElement fe : context.getPathElements()) {
                    System.out.println(fe.getName() + "  - " + fe.eClass().getName());
                }
            }
            if (jsonPaths != null) {
                System.out.println("$$$ AS JSON:");
                System.out.println(jsonPaths.toString());
                System.out.println("$$$$$$$$");
            }
            System.out.println("#####################################################");
        }
    }
    
    public static StatefulKnowledgeSession createSession(String process) {
        KnowledgeBuilder builder = KnowledgeBuilderFactory.newKnowledgeBuilder();
        
        builder.add(ResourceFactory.newClassPathResource(process), ResourceType.BPMN2);
        
        KnowledgeBase kbase = builder.newKnowledgeBase();
        KieSessionConfiguration config = KnowledgeBaseFactory.newKnowledgeSessionConfiguration();
        config.setOption(ClockTypeOption.get("pseudo") );
        StatefulKnowledgeSession session = kbase.newStatefulKnowledgeSession(config, EnvironmentFactory.newEnvironment());
        NodeInstanceFactoryRegistry n = NodeInstanceFactoryRegistry.INSTANCE;
        
        n.register( RuleSetNode.class,
                  new CreateNewNodeFactory( SimulationNodeInstance.class ) );
        n.register( Split.class,
                  new CreateNewNodeFactory( SimulationNodeInstance.class ) );
        n.register( Join.class,
                  new ReuseNodeFactory( SimulationNodeInstance.class ) );
        n.register( StartNode.class,
                  new CreateNewNodeFactory( SimulationStartNodeInstance.class ) );
        n.register( EndNode.class,
                  new CreateNewNodeFactory( SimulationEndNodeInstance.class ) );
        n.register( MilestoneNode.class,
                  new CreateNewNodeFactory( SimulationNodeInstance.class ) );
        n.register( SubProcessNode.class,
                  new CreateNewNodeFactory( SimulationNodeInstance.class ) );
        n.register( ActionNode.class,
                  new CreateNewNodeFactory( SimulationNodeInstance.class ) );
        n.register( WorkItemNode.class,
                  new CreateNewNodeFactory( SimulationNodeInstance.class ) );
        n.register( TimerNode.class,
                  new CreateNewNodeFactory( SimulationNodeInstance.class ) );
        n.register( FaultNode.class,
                  new CreateNewNodeFactory( SimulationNodeInstance.class ) );
        n.register( CompositeNode.class,
                  new CreateNewNodeFactory( SimulationNodeInstance.class ) );
        n.register( CompositeContextNode.class,
                  new CreateNewNodeFactory( SimulationNodeInstance.class ) );
        n.register( HumanTaskNode.class,
                  new CreateNewNodeFactory( SimulationNodeInstance.class ) );
        n.register( ForEachNode.class,
                  new CreateNewNodeFactory( SimulationNodeInstance.class ) );
        n.register( EventNode.class,
                  new CreateNewNodeFactory( SimulationNodeInstance.class ) );
        n.register( StateNode.class,
                  new CreateNewNodeFactory( SimulationNodeInstance.class ) );
        n.register( DynamicNode.class,
                  new CreateNewNodeFactory( SimulationNodeInstance.class ) );
        
        n.register(CatchLinkNode.class, new CreateNewNodeFactory(
                SimulationNodeInstance.class));
        n.register(ThrowLinkNode.class, new CreateNewNodeFactory(
                SimulationNodeInstance.class));
        return session;
    }
    
    public static List<FlowElement> removeDuplicates(Set<FlowElement> orig) {
        
        Set<String> uniqueIds = new HashSet<String>();
        List<FlowElement> unique = new ArrayList<FlowElement>();
        
        for (FlowElement fElement : orig) {
            if (!uniqueIds.contains(fElement.getId())) {
                uniqueIds.add(fElement.getId());
                unique.add(fElement);
            }
        }
        System.out.println("Size of flow elements after removing duplicates " + unique.size());
        return unique;
    }
    
    public static ReleaseId createKJarWithMultipleResources(String id, String[] resourceFiles, ResourceType[] types) throws IOException {
        KieServices ks = KieServices.Factory.get();
        KieModuleModel kproj = ks.newKieModuleModel();
        KieFileSystem kfs = ks.newKieFileSystem();

        for (int i = 0; i < resourceFiles.length; i++) {            
            Resource resource = ResourceFactory.newClassPathResource(resourceFiles[i]);
            String res = readResourceContent(resource);
            String type = types[i].getDefaultExtension();

            kfs.write("src/main/resources/" + id.replaceAll("\\.", "/")
                    + "/org/test/res" + i + "." + type, res);
        }

        KieBaseModel kBase1 = kproj.newKieBaseModel(id)
                .setEqualsBehavior(EqualityBehaviorOption.EQUALITY)
                .setEventProcessingMode(EventProcessingOption.STREAM);

        KieSessionModel ksession1 = kBase1
                .newKieSessionModel(id + ".KSession1")
                .setType(KieSessionModel.KieSessionType.STATEFUL)
                .setClockType(ClockTypeOption.get("pseudo"));

        kfs.writeKModuleXML(kproj.toXML());

        KieBuilder kieBuilder = ks.newKieBuilder(kfs).buildAll();
        assertTrue(kieBuilder.getResults().getMessages().isEmpty());

        KieModule kieModule = kieBuilder.getKieModule();
        return kieModule.getReleaseId();
    }
    
    protected static String readResourceContent(Resource resource) {
        StringBuilder contents = new StringBuilder();
        BufferedReader reader = null;
 
        try {
            reader = new BufferedReader(resource.getReader());
            String text = null;
 
            // repeat until all lines is read
            while ((text = reader.readLine()) != null) {
                contents.append(text);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        return contents.toString();
    }
}
