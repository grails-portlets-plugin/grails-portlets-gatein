package org.codehaus.grails.portlets.container.gatein;


import java.lang.reflect.Field
import javax.portlet.PortletConfig
import javax.portlet.PortletContext
import javax.portlet.PortletRequest
import javax.portlet.PortletResponse
import javax.servlet.ServletConfig
import javax.servlet.ServletContext
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import org.codehaus.grails.portlets.container.AbstractPortletContainerAdapter

/**
 * @author Jakob Munih <munih@parsek.net>
 */
public class GateinPortletContainerAdapter extends AbstractPortletContainerAdapter {

   public ServletContext getServletContext(PortletContext context) throws UnsupportedOperationException {
      Field privateServletContextField = context.class.getDeclaredField("servletContext");
      privateServletContextField.setAccessible(true);
      return privateServletContextField.get(context);
   }

   public ServletConfig getServletConfig(PortletConfig config) throws UnsupportedOperationException {
      throw new UnsupportedOperationException("GateIn doesn't wrap ServletConfig in PortletConfigImpl.")
   }

   public HttpServletRequest getHttpServletRequest(PortletRequest portletRequest) throws UnsupportedOperationException {
      return portletRequest.getRealRequest();
   }

   public HttpServletResponse getHttpServletResponse(PortletResponse portletResponse) throws UnsupportedOperationException {
      return portletResponse.getRealResponse();
   }

}
