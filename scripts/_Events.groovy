import groovy.xml.StreamingMarkupBuilder
import org.codehaus.groovy.grails.commons.GrailsResourceUtils

def portletVersion = '1.0'
def basedir = System.getProperty("base.dir")
def portletXml = new File("${basedir}/web-app/WEB-INF/portlet.xml")

// Those jars are loaded at parents classloaders, 
// and causes silent portlet deployment failure if included.
eventCreateWarStart = { warName, stagingDir ->
   ant.delete(failonerror: false) {
      fileset(dir: "${stagingDir}/WEB-INF/lib") {
         // Portlet specific
         include(name: "servlet-api*.jar")
         include(name: "portlet-api*.jar")
         include(name: "jcl-over-slf4j*.jar")
         // GateIN/JBOSS specific
         // more info at http://blog.saddey.net/2010/03/06/how-to-deploy-a-grails-application-to-jboss-5/
         include(name: "*log4j*.jar")
         include(name: "*slf4j*.jar")
         include(name: "hsqldb-*.jar")
      }
   }
}

// Add portlet's xmls
eventPackagingEnd = {

   // Skip action when packagin plugin
   String searchPath = "${basedir}/grails-app/portlets"
   if (searchPath.contains("portlets-gatein")) {
      return true;
   }

   try {
      def xmlWriter = new StreamingMarkupBuilder();
      def customModes = [:]
      def userAttributes = [:]
      event("StatusUpdate", ["Searching for portlets: ${searchPath}"])
      portletFiles = resolveResources(searchPath)
      if (portletFiles.size() > 0) {
         event("StatusUpdate", ["Generating portlet.xml - ${portletFiles.size()} portlets found"])

         if (portletXml.exists()) portletXml.delete()
         def underscoredVersion = portletVersion.replaceAll("\\.", "_")
         def xml = xmlWriter.bind {
            'portlet-app'(version: portletVersion,
               xmlns: "http://java.sun.com/xml/ns/portlet/portlet-app_${underscoredVersion}.xsd",
               'xmlns:xsi': 'http://www.w3.org/2001/XMLSchema-instance',
               'xsi:schemaLocation': "http://java.sun.com/xml/ns/portlet/portlet-app_${underscoredVersion}.xsd") {
               mkp.comment 'GENERATED BY GRAILS PORTLETS PLUGIN - DO NOT EDIT'
               portletFiles.each {portletClassFile ->
                  def className = GrailsResourceUtils.getClassName(portletClassFile.getPath())
                  Class portletClass = classLoader.loadClass(className)
                  def portletName = className - 'Portlet'
                  def instance = portletClass.newInstance()
                  checkRequiredProperties(['supports', 'title', 'displayName'], instance)
                  //TODO security constraints
                  portlet {
                     'portlet-name'(portletName)
                     'display-name'(instance.displayName)
                     if (hasProperty('description', instance))
                        'description'(instance.description)
                     'portlet-class'('org.codehaus.grails.portlets.GrailsDispatcherPortlet')
                     'init-param'
                        {
                           'name'('contextClass')
                           'value'('org.codehaus.grails.portlets.GrailsPortletApplicationContext')
                        }
                     'init-param'
                        {
                           'name'('grailsPortletClass')
                           'value'(className)
                        }
                     'init-param'
                        {
                           'name'('contextConfigLocation')
                           'value'('/WEB-INF/portlet-context.groovy')
                        }
                     instance.supports.each {mime, types ->
                        'supports'
                           {
                              'mime-type'(mime)
                              types.each {mode ->
                                 'portlet-mode'(mode)
                              }
                           }
                     }
                     if (hasProperty('customModes', instance) && instance.customModes instanceof Map) {
                        customModes += instance.supportsCustom
                     }
                     'portlet-info'
                        {
                           //TODO support 1l8n via properties files to supply these
                           'title'(instance.title)
                           if (hasProperty('shortTitle', instance)) 'short-title'(instance.shortTitle)
                           if (hasProperty('keywords', instance)) 'keywords'(instance.keywords)
                        }
                     if (hasProperty('roleRefs', instance) && instance.roleRefs instanceof List) {
                        instance.roleRefs.each {roleName ->
                           'security-role-ref'
                              {
                                 'role-name'(roleName)
                              }
                        }
                     }
                     if (hasProperty('userAttributes', instance) && instance.userAttributes instanceof List) {
                        userAttributes += instance.userAttributes

                     }
                     if (hasProperty('supportedPreferences', instance) && instance.supportedPreferences instanceof Map) {
                        'portlet-preferences'
                           {
                              instance.supportedPreferences.each {prefName, prefValue ->
                                 'preference'
                                    {
                                       'name'(prefName)
                                       if (prefValue instanceof List) {
                                          prefValue.each {multiValue ->
                                             'value'(multiValue)
                                          }
                                       } else {
                                          'value'(prefValue)
                                       }
                                       /* TODO
                                       if (preference.readOnly) {
                                           'read-only'('true')
                                       }*/
                                    }
                              }
                           }
                     }
                  }
               }
               userAttributes.each {userAttribute ->
                  'user-attribute'
                     {
                        'name'(userAttribute)
                     }
               }
               customModes.each {mode, description ->
                  'custom-portlet-mode'
                     {
                        'description'(description)
                        'name'(mode)
                     }
               }

            }
         }
         portletXml.write(xml.toString())
      }
      def spring_conf = "${portletsGateinPluginDir}/src/templates/scripts/portlet-context.groovy"
      if (new File(spring_conf).exists()) {
         ant.copy(file: spring_conf,
            todir: "${basedir}/web-app/WEB-INF")
      }
   } catch (Exception e) {
      event("StatusError", ["Unable to generate portlet.xml: " + e.message])
      exit(1)
   }

}

/**
 * TODO JBOSS - Remove log4j configuration stuff (when running with JBoss or GlassFish a.s.o)
 */
eventWebXmlEnd = {String tmpfile ->

   def root = new XmlSlurper().parse(webXmlFile)

   // When running with JBoss (or GlassFish a.s.o) remove log4j configuration stuff
   def log4j = root.listener.findAll {node ->
      node.'listener-class'.text() == 'org.codehaus.groovy.grails.web.util.Log4jConfigListener'
   }
   log4j.replaceNode {}

   def log4jFile = root.'context-param'.findAll {node ->
      node.'param-name'.text() == 'log4jConfigLocation'
   }
   log4jFile.replaceNode {}

   webXmlFile.text = new StreamingMarkupBuilder().bind {
      mkp.declareNamespace("": "http://java.sun.com/xml/ns/j2ee")
      mkp.yield(root)
   }
}

def hasProperty(propertyName, instance) {
   try {
      def value = instance."${propertyName}"
      return true;
   } catch (MissingPropertyException mpe) {
      return false;
   }
}

def checkRequiredProperties(propertyNames, instance) {
   propertyNames.each {
      if (!hasProperty(it, instance)) {
         throw new MissingPropertyException("${instance.class.name} does not have the required properties ${propertyNames}",
            instance.class)
      }
   }
}

def resolveResources(String directory) {

   def portlets = []
   new File(directory).eachDirRecurse() { dir ->
      println "reading ${dir}"
      dir.eachFileMatch(~/.*Portlet.groovy/) { file ->
         portlets << file
      }
   }

   return portlets
}
