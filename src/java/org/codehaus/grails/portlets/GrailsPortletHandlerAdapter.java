package org.codehaus.grails.portlets;

import groovy.lang.Closure;
import groovy.lang.GroovyObject;
import groovy.util.ConfigObject;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.ConfigurationHolder;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.view.GroovyPageView;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.portlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;

import javax.portlet.*;
import java.util.Map;

import static grails.util.GrailsNameUtils.getShortName;

/**
 * @author Lee Butts
 */
public class GrailsPortletHandlerAdapter implements org.springframework.web.portlet.HandlerAdapter,
        ApplicationContextAware {
   private ApplicationContext applicationContext;
   private Log log = LogFactory.getLog(this.getClass());

   public boolean supports(Object o) {
      return o instanceof GroovyObject;
   }

   public void handleAction(ActionRequest actionRequest, ActionResponse actionResponse, Object o) throws Exception {
      GroovyObject portlet = (GroovyObject) o;
      String action = actionRequest.getParameter("action");
      Closure actionClosure = getActionClosure(actionRequest, portlet, action);
      actionClosure.call();
   }

   /* TODO */
   public void handleEvent(EventRequest eventRequest, EventResponse eventResponse, Object o) throws Exception {
      throw new Exception("handleEvent in Grails portlets is not implemented yet");
   }

   /* TODO */
   public ModelAndView handleResource(ResourceRequest handleRequest, ResourceResponse handleResponse, Object o) throws Exception {
      throw new Exception("handleResource in Grails portlets is not implemented yet");
   }

   private Closure getActionClosure(PortletRequest request, GroovyObject portlet, String actionParameter) {
      return getPortletClosure(request, portlet, actionParameter, "action");
   }

   private Closure getRenderClosure(PortletRequest request, GroovyObject portlet, String actionParameter) {
      return getPortletClosure(request, portlet, actionParameter, "render");
   }

   private Closure getPortletClosure(PortletRequest request, GroovyObject portlet, String actionParameter, String closurePrefix) {
      Closure portletClosure = null;
      if (actionParameter != null) {
         try {
            portletClosure = (Closure) portlet.getProperty(actionParameter);
         } catch (Exception e) {
            log.warn("Unable to find Closure property "
                    + actionParameter + " from action request parameter");
         }
      }
      if (portletClosure == null) {
         String portletMode = request.getPortletMode().toString().toLowerCase();
         String modeActionName = closurePrefix + StringUtils.capitalize(portletMode);
         try {
            portletClosure = (Closure) portlet.getProperty(modeActionName);
         } catch (Exception e) {
            log.trace("Didn't find portlet mode " + closurePrefix + " closure: " + modeActionName);
         }
      }
      if (portletClosure == null) {
         String defaultParam = "do" + StringUtils.capitalize(closurePrefix);
         log.info("Falling back to " + defaultParam + " closure");
         portletClosure = (Closure) portlet.getProperty(defaultParam);
      }
      return portletClosure;
   }

   public ModelAndView handleRender(RenderRequest renderRequest, RenderResponse renderResponse, Object o) throws Exception {
      if (getMinimisedConfig() != null && renderRequest.getWindowState().equals(WindowState.MINIMIZED)) {
         log.info("portlet.handleMinimised is set, rendering empty string");
         renderResponse.setContentType("text/html");
         renderResponse.getPortletOutputStream().write("".getBytes());
         return null;
      } else {
         GroovyObject portlet = (GroovyObject) o;
         String action = renderRequest.getParameter("action");
         Closure render = getRenderClosure(renderRequest, portlet, action);
         Object returnValue = render.call();
         if (returnValue instanceof Map) {
            ((Map) returnValue).put("portletRequest", renderRequest);
            ((Map) returnValue).put("portletResponse", renderResponse);
            renderRequest.setAttribute(GrailsApplicationAttributes.CONTROLLER, portlet);
            String viewName = "/" + getViewDir(renderRequest) + "/" + renderRequest.getParameter("action");
            if (tryResolveView(viewName)) {
               log.info("Trying to render action view " + viewName);
            } else {
               log.info("Couldn't resolve action view " + viewName);
               viewName = "/" + getViewDir(renderRequest) + "/" + renderRequest.getPortletMode().toString().toLowerCase();
               if (tryResolveView(viewName)) {
                  log.info("Trying to render mode view " + viewName);
               } else {
                  log.info("Couldn't resolve mode view " + viewName);
                  viewName = "/" + getViewDir(renderRequest) + "/render";
                  log.info("Trying to render view " + viewName);
               }
               return new ModelAndView(viewName, (Map) returnValue);
            }
         }
         return null;
      }
   }

   private String getViewDir(RenderRequest renderRequest) {
      String shortName = getShortName(((String) renderRequest.getAttribute(GrailsDispatcherPortlet.PORTLET_NAME)));
      return shortName.substring(0,1).toLowerCase() + shortName.substring(1);
   }

   private Object getMinimisedConfig() {
      try {
         //TODO allow overriding config setting per portlet
         ConfigObject configObject = (ConfigObject) ConfigurationHolder.getConfig().get("portlet");
         Object value = null;
         if (configObject != null) {
            value = configObject.get("handleMinimised");
         }
         if (value != null) {
            return value;
         } else {
            log.info("portlet.handleMinimised not set, proceeding with normal render");
            return null;
         }
      } catch (ClassCastException e) {
         log.warn("Unable to determine portlet.handleMinimised setting");
         return null;
      }
   }

   public boolean tryResolveView(String viewName) {
      ViewResolver vr = (ViewResolver) applicationContext.getBean("jspViewResolver");
      try {
         View view = vr.resolveViewName(viewName, LocaleContextHolder.getLocaleContext().getLocale());
         return view instanceof GroovyPageView; // GrailsViewResolver will return a GPV if it exists otherwise it's a normal JSP view (which may or may not exist)
      } catch (Exception e) {
         return false;
      }
   }

   public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
      this.applicationContext = applicationContext;
   }
}
