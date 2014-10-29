<%--

     NEXUSe2e Business Messaging Open Source
     Copyright 2000-2009, Tamgroup and X-ioma GmbH

     This is free software; you can redistribute it and/or modify it
     under the terms of the GNU Lesser General Public License as
     published by the Free Software Foundation version 2.1 of
     the License.

     This software is distributed in the hope that it will be useful,
     but WITHOUT ANY WARRANTY; without even the implied warranty of
     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
     Lesser General Public License for more details.

     You should have received a copy of the GNU Lesser General Public
     License along with this software; if not, write to the Free
     Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
     02110-1301 USA, or see the FSF site: http://www.fsf.org.

--%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@page import="org.nexuse2e.Version"%>
<%@ page import="java.io.FileReader" %>
<%@ page import="java.io.BufferedReader" %>
<%@ page import="java.io.IOException" %>

<%
     String machineName = null;
     String filePath = "";
     String configPath = System.getProperty("externalconfig");
     if (null != configPath && !"".equals(configPath)) {
          try {
               filePath = filePath.concat(configPath).concat("\\machine_name.txt");
               BufferedReader br = new BufferedReader(new FileReader(filePath));
               machineName = br.readLine();
          } catch (IOException ioe) {
               try {
                    filePath = filePath.concat(configPath).concat("../config/machine_name.txt");
                    BufferedReader br = new BufferedReader(new FileReader(filePath));
                    machineName = br.readLine();
               } catch (IOException ioeInternal) {
                    machineName = "NEXUSe2e";
               }

          }
     }

%>

<div id="logo_div">
</div>
<div id="machine_div_1">
     <c:set var="machine_name"><%= machineName %></c:set>
     <c:out value="${machine_name}" />
</div>
<div id="machine_div_2">
	<%= Version.getVersion() %>
</div>
<div id="logged_in_user">
	<c:if test="${not empty nxUser}">
		You're logged in as ${nxUser.firstName} ${nxUser.lastName}
	</c:if>
</div>