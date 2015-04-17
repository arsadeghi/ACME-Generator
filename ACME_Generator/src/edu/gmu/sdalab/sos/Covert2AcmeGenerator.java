package edu.gmu.sdalab.sos;

import static edu.gmu.archextractor.Main.FILTER_NO;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBException;

import edu.gmu.archextractor.alloy.filter.NoFilter;
import edu.gmu.archextractor.sca.modelExtractor.ContentProvider;
import edu.gmu.archextractor.sca.modelExtractor.Intent;
import edu.gmu.archextractor.structures.Application;
import edu.gmu.archextractor.structures.Component;
import edu.gmu.archextractor.structures.Component.TYPE;
import edu.gmu.archextractor.structures.IntentFilter;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.Version;

/**
 * @author Alireza Sadeghi SDA Lab
 *
 */
public class Covert2AcmeGenerator {

	private final static String EXPLICIT_INTENT_COMPONENT_RESPONSE_PORT_NAME = "ExplicitIntentResponsePortT0";
	private final static String IMPLICIT_INTENT_COMPONENT_BROADCAST_RECEIVE_PORT_NAME = "ImplicitIntentBroadcastReceivePortT0";
	private final static String CONTENT_PROVIDER_COMPONENT_RESPONSE_PORT_NAME = "ContentProviderResponsePortT";
	private final static String INTENT_CALL_CONNECTOR_RESPONSE_PORT = "response";
	private final static String INTENT_CALL_CONNECTOR_CALL_PORT = "call";
	private final static String CONTENT_PROVIDER_CONNECTOR_RESPONSE_PORT = "repository";
	private final static String CONTENT_PROVIDER_CONNECTOR_REQUEST_PORT = "access";
	private final static String IMPLICIT_INTENT_BUS_NAME = "ImplicitIntentBus";

	private static Configuration cfg;
	private Application application;
	Map<String, Object> app;
	List<Map<String, Object>> components;
	List<Map<String, Object>> connectors;
	List<Map<String, Object>> attachments;
	Set<String> targetComponents;
	int nextExplicitIntentResponsePortIndex;
	int nextImplicitIntentBroadcastAnnouncePortIndex;
	int nextImplicitIntentBroadcastReceivePortIndex;
	int nextContentProviderResponsePortIndex;

	int nextExplicitIntentCallComponentPortIndex;
	int nextImplicitIntentBroadcastAnnounceComponentPortIndex;
	int nextContentProviderCallComponentPortIndex;

	public static void main(String[] args) throws Exception {
		covert2Acme(args[0], args[1], args[2]);
	}

	public Covert2AcmeGenerator(Application application) {
		this.application = application;
		this.app = new HashMap<>();
		this.components = new ArrayList<>();
		this.connectors = new ArrayList<>();
		this.attachments = new ArrayList<>();
		this.targetComponents = new HashSet<String>();
		this.nextExplicitIntentResponsePortIndex = 0;
		this.nextImplicitIntentBroadcastAnnouncePortIndex = 0;
		this.nextImplicitIntentBroadcastReceivePortIndex = 0;
		this.nextContentProviderResponsePortIndex = 0;
	}

	public static void covert2Acme(String apkFile, String outputFolder, String templateDir) throws JAXBException, IOException, TemplateException {
		Application app = Application.fromXML(new File(apkFile));
		cfg = getConfigurationInstance(templateDir);
		Covert2AcmeGenerator generator = new Covert2AcmeGenerator(app);
		generator.processApp(outputFolder);
	}

	private static Configuration getConfigurationInstance(String destDir) throws IOException {
		if (cfg == null) {
			try {
				cfg = new Configuration();
			} catch (Exception e) {
				e.printStackTrace();
			}
			cfg.setDirectoryForTemplateLoading(new File(destDir));
			cfg.setObjectWrapper(new DefaultObjectWrapper());
			cfg.setDefaultEncoding("UTF-8");
			cfg.setTemplateExceptionHandler(TemplateExceptionHandler.HTML_DEBUG_HANDLER);
			cfg.setIncompatibleImprovements(new Version(2, 3, 20));
		}
		return cfg;
	}

	private void processApp(String outputFolder) throws IOException, TemplateException {
		for (Intent intent : application.getNewIntents())
			if (intent.isExplicit())
				targetComponents.add(intent.getComponent());
		for (Component c : application.getMergedComponents())
			processComponent(c);
		app.put("appName", application.getFullName());
		app.put("usesPerms", application.getUsesPermissions());
		app.put("components", components);
		app.put("connectors", connectors);
		app.put("attachments", attachments);
		writeOutput(app, outputFolder, "app", application.getFullName());
	}

	private void processComponent(Component c) {
		HashMap<String, Object> component = new HashMap<>();
		nextExplicitIntentCallComponentPortIndex = 0;
		nextImplicitIntentBroadcastAnnounceComponentPortIndex = 0;
		nextContentProviderCallComponentPortIndex = 0;
		TYPE type = c.getComponentType();
		component.put("type", type.name().toLowerCase());
		component.put("typeName", type.title);
		component.put("name", c.getSimpleName());
		component.put("class", c.getName());
		// TODO Explicitly Exported
		component.put("exported", c.isExportedComponent());
		if (type == TYPE.PROVIDER) {
			component.put("readPermission", c.getReadPrm());
			component.put("writePermission", c.getWritePrm());
			//TODO Content provider Entry points 
			//component.put("ContentProviderResponsePortT", CONTENT_PROVIDER_COMPONENT_RESPONSE_PORT_NAME);
		} else {
			component.put("permission", c.getPermissionsStr());
		}
		Set<String> intentFilters = new HashSet<>();
		for (IntentFilter filter : c.getIntentFilters(FILTER_NO)) {
			for (String action : filter.getActions())
				intentFilters.add(action);
		}
		component.put("filters", intentFilters);
		if (targetComponents.contains(c.getName()))
			component.put("ExplicitIntentResponsePortT", EXPLICIT_INTENT_COMPONENT_RESPONSE_PORT_NAME);
		if (!intentFilters.isEmpty()) {
			component.put("ImplicitIntentBroadcastReceivePortT", IMPLICIT_INTENT_COMPONENT_BROADCAST_RECEIVE_PORT_NAME);
			String responseConnector = "ImplicitIntentBroadcastReceiveRoleT" + (nextImplicitIntentBroadcastReceivePortIndex++);
			createConnector(responseConnector, ConnectorTypes.ImplicitIntentBroadcastReceive);
			attachPorts(c.getSimpleName(), IMPLICIT_INTENT_COMPONENT_BROADCAST_RECEIVE_PORT_NAME, IMPLICIT_INTENT_BUS_NAME, responseConnector);
		}
		processIntents(component, c);
		//processProviders(component, c);
		components.add(component);
	}

	private void processIntents(HashMap<String, Object> component, Component c) {
		List<HashMap<String, Object>> explicitIntentCalls = new ArrayList<>();
		List<HashMap<String, Object>> implicitIntentCalls = new ArrayList<>();
		Set<Intent> addedIntents = new HashSet<>();
		for (Intent i : application.getNewIntentsByComponents(c)) {
			if (!new NoFilter().shouldNotFilter(i))
				continue;
			if (!(i.hasValidReceiverComponent(application.getComponents()) && i.hasValidSenderComponent(application.getComponents())))
				continue;
			if (containsIntent(i, addedIntents))
				continue;
			addedIntents.add(i);
			HashMap<String, Object> intent = new HashMap<>();
			intent.put("action", i.getAction().replaceAll("\"", ""));
			intent.put("category", i.getCategories().replaceAll("\"", ""));
			if (i.isExplicit()) {
				String callPort = "ExplicitIntentCallPortT" + (nextExplicitIntentCallComponentPortIndex++);
				intent.put("name", callPort);
				intent.put("componentReference", i.getComponent());
				explicitIntentCalls.add(intent);
				String connectorName = "IntentCallResponseConnectorT" + (nextExplicitIntentResponsePortIndex++);
				createConnector(connectorName, ConnectorTypes.IntentCallResponseConnector, "callPort", INTENT_CALL_CONNECTOR_CALL_PORT, "responsePort",
						INTENT_CALL_CONNECTOR_RESPONSE_PORT);
				attachPorts(i.getSimpleSender(), callPort, connectorName, INTENT_CALL_CONNECTOR_CALL_PORT);
				attachPorts(i.getSimpleComponent(), EXPLICIT_INTENT_COMPONENT_RESPONSE_PORT_NAME, connectorName, INTENT_CALL_CONNECTOR_RESPONSE_PORT);
			} else {
				String announcePort = "ImplicitIntentBroadcastAnnouncerPortT" + (nextImplicitIntentBroadcastAnnounceComponentPortIndex++);
				intent.put("name", announcePort);
				implicitIntentCalls.add(intent);
				String connectorName = "ImplicitIntentBroadcastAnnounceRoleT" + (nextImplicitIntentBroadcastAnnouncePortIndex++);
				createConnector(connectorName, ConnectorTypes.ImplicitIntentBroadcastAnnounce);
				attachPorts(i.getSimpleSender(), announcePort, IMPLICIT_INTENT_BUS_NAME, connectorName);
			}
		}
		component.put("explicitIntentCalls", explicitIntentCalls);
		component.put("implicitIntentCalls", implicitIntentCalls);
	}

	//TODO Content Provider calls
	private void processProviders(HashMap<String, Object> component, Component c) {
		List<String> providerCalls = new ArrayList<>();
		for (ContentProvider p : application.getProvierByComponents(c)) {
			String requestProvider = "ContentProviderCallPortT" + (nextContentProviderCallComponentPortIndex++);
			providerCalls.add(requestProvider);
			String connectorName = "ContentProviderConnectorT" + (nextContentProviderResponsePortIndex++);
			createConnector(connectorName, ConnectorTypes.ContentProviderConnector, "repository", CONTENT_PROVIDER_CONNECTOR_RESPONSE_PORT, "access",
					CONTENT_PROVIDER_CONNECTOR_REQUEST_PORT);
			attachPorts(p.getSimpleCaller(), requestProvider, connectorName, CONTENT_PROVIDER_CONNECTOR_REQUEST_PORT);
			// TODO Add content provider repository
			attachPorts(p.getRepository(), CONTENT_PROVIDER_COMPONENT_RESPONSE_PORT_NAME, connectorName, CONTENT_PROVIDER_CONNECTOR_RESPONSE_PORT);
		}
		component.put("providerCalls", providerCalls);
	}

	private void createConnector(String name, ConnectorTypes type) {
		createConnector(name, type, null, null, null, null);
	}

	private void createConnector(String name, ConnectorTypes type, String port1Name, String port1Value, String port2Name, String port2Value) {
		HashMap<String, Object> connector = new HashMap<>();
		connector.put("name", name);
		connector.put("type", type.name());
		if (port1Name != null)
			connector.put(port1Name, port1Value);
		if (port2Name != null)
			connector.put(port2Name, port2Value);
		connectors.add(connector);
	}

	private void attachPorts(String fromComponent, String fromPort, String toConnector, String toPort) {
		HashMap<String, Object> attachment = new HashMap<>();
		attachment.put("from", fromComponent + "." + fromPort);
		attachment.put("to", toConnector + "." + toPort);
		attachments.add(attachment);
	}

	enum ConnectorTypes {
		IntentCallResponseConnector, ContentProviderConnector, ImplicitIntentBroadcastAnnounce, ImplicitIntentBroadcastReceive;
	}

	private static void writeOutput(Map<String, Object> root, String outputDir, String templateFile, String outputFile) throws IOException, TemplateException {
		Template template = cfg.getTemplate(templateFile + ".ftl");
		FileWriter writer = new FileWriter(new File(outputDir, outputFile + ".acme"));
		template.process(root, writer);
	}

	public boolean containsIntent(Intent intent, Set<Intent> intents) {
		// Only for ACME model as it just compares action, category and
		// component
		for (Intent i : intents) {
			if (i.getAction().equals(intent.getAction()) && i.getCategories().equals(intent.getCategories()) && i.getComponent().equals(intent.getComponent()))
				return true;
		}
		return false;
	}
}
