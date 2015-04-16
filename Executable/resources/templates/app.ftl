//Automatically Generated by Covert Engine
import families/Android.acme;
system ${appName} : Android = new Android extended with {
   //List of Components 
   <#list components as component>
   component ${component.name} : ${component.typeName}ComponentT = new ${component.typeName}ComponentT extended with {
      property class = "${component.class}";
      property exported = ${component.exported?c};
      property intentFilters = {
         <#list component.filters as filter>"${filter}"<#if filter_has_next>, </#if></#list>
      };
      <#if component.type = "activity">property state = Created;</#if>  
      <#if component.type = "provider">
      <#if (component.readPermission)?has_content>property read-permission = "${component.readPermission}";</#if>
      <#if (component.writePermission)?has_content>property write-permission = "${component.writePermission}";</#if>
      <#else>
      <#if (component.permission)?has_content>property permission = "${component.permission}";</#if>
      </#if>     
      <#if component.ContentProviderResponsePortT??>port ${component.ContentProviderResponsePortT} : ContentProviderResponsePortT = new ContentProviderResponsePortT;</#if>
      <#if component.ExplicitIntentResponsePortT??>port ${component.ExplicitIntentResponsePortT} : ExplicitIntentResponsePortT = new ExplicitIntentResponsePortT;</#if>
      <#if component.ImplicitIntentBroadcastReceivePortT??>port ${component.ImplicitIntentBroadcastReceivePortT} : ImplicitIntentBroadcastReceivePortT = new ImplicitIntentBroadcastReceivePortT;</#if>
      <#list component.implicitIntentCalls as intent>
      port ${intent.name} : ImplicitIntentBroadcastAnnouncerPortT = new ImplicitIntentBroadcastAnnouncerPortT extended with {
        <#if (intent.action)?has_content>property action = "${intent.action}";
        </#if><#if (intent.category)?has_content>property category = "${intent.category}";</#if>
      };
      </#list>
      <#list component.explicitIntentCalls as intent>
      port ${intent.name} : ExplicitIntentCallPortT = new ExplicitIntentCallPortT extended with {
        <#if (intent.action)?has_content>property action = "${intent.action}";
        </#if><#if (intent.category)?has_content>property category = "${intent.category}";
        </#if><#if intent.componentReference??>property componentReference = "${intent.componentReference}";</#if>
      };
      </#list>
      <#list component.providerCalls as providerCall>
      port ${providerCall} : ContentProviderCallPortT = new ContentProviderCallPortT;
      </#list>
   };
   </#list>
   
   //List of Explicit Connectors 
   <#list connectors as connector>
   <#if connector.type = "IntentCallResponseConnector">
   connector ${connector.name} : IntentCallResponseConnectorT = new IntentCallResponseConnectorT extended with {
    role ${connector.responsePort} : ExplicitIntentResponseRoleT = new ExplicitIntentResponseRoleT;
    role ${connector.callPort} : ExplicitIntentCallRoleT = new ExplicitIntentCallRoleT;
   };    
   </#if>
   </#list>
   
   //List of Content Provier Connectors 
   <#list connectors as connector>
   <#if connector.type = "ContentProviderConnector">
   connector ${connector.name} : ContentProviderConnectorT = new ContentProviderConnectorT extended with {
    role ${connector.repository} : ContentProviderResponseRoleT = new ContentProviderResponseRoleT;
    role ${connector.access} : ContentProviderRequestRoleT = new ContentProviderRequestRoleT;
   };
   </#if>
   </#list>

   //Implicit Intent Bus Connector
   connector ImplicitIntentBus : IntentBusT = new IntentBusT extended with {
   <#list connectors as connector>
   <#if connector.type = "ImplicitIntentBroadcastAnnounce">
     role ${connector.name} : ImplicitIntentBroadcastAnnounceRoleT = new ImplicitIntentBroadcastAnnounceRoleT;
   <#elseif connector.type = "ImplicitIntentBroadcastReceive">
     role ${connector.name} : ImplicitIntentBroadcastReceiveRoleT = new ImplicitIntentBroadcastReceiveRoleT;
   </#if>
   </#list>
   };
    
   //Components to Connectors Attachments
   <#list attachments as attachment>
   attachment ${attachment.from} to ${attachment.to};
   </#list>
       
   
   group ${appName} : AndroidApplicationGroupT = new AndroidApplicationGroupT extended with {
    property usesPermissions = {
         <#list usesPerms as perm>"${perm}"<#if perm_has_next>,</#if></#list>};
    members {
         <#list components as comp>${comp.name}<#if comp_has_next>,</#if></#list>}};
}