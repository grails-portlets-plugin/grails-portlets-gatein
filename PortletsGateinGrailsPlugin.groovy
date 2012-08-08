class PortletsGateinGrailsPlugin {

   // the plugin version
   def version = "0.3"
   
   // the version or versions of Grails the plugin is designed for
   def grailsVersion = "2.0 > *"
   
   // the other plugins this plugin depends on
   def loadAfter = ['controllers']
   
   // the other plugins this plugin depends on
    def dependsOn = [portlets:"0.9.2 > * "]

   // resources that are excluded from plugin packaging
   def pluginExcludes = [
           "grails-app/views/error.gsp"
   ]

   def author = "Jakob Munih"
   def authorEmail = "munih@parsek.net"
   def title = "Gatein Portlets Plugin"
   def description = '''\\
Generate JSR-168 compliant portlet war to use it in GateIN portal 3.1. 
Based on original Grails portlets plugin.
'''

   // URL to the plugin's documentation
   def documentation = "http://grails.org/plugins/portlets-gatein"

   def watchedResources = [
           'file:./grails-app/portlets/**/*Portlet.groovy',
           'file:./plugins/*/grails-app/portlets/**/*Portlet.groovy'
   ]

   def doWithSpring = {
      portletContainerAdapter(org.codehaus.grails.portlets.container.gatein.GateinPortletContainerAdapter)
   }

   def doWithApplicationContext = { applicationContext ->
   }

   def doWithWebDescriptor = { xml ->
   }

   def doWithDynamicMethods = { ctx ->
   }

   def onChange = { event ->
   }

   def onConfigChange = { event ->
   }
}
