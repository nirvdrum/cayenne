<%@ page 
	language="java" 
	contentType="text/html"
	import="javax.naming.InitialContext,
		org.objectstyle.cayenne.examples.ejbfacade.interfaces.*,
		org.objectstyle.cayenne.examples.ejbfacade.model.*,
		java.util.*"
%>

<%
InitialContext context = new InitialContext();
AuctionSessionHome auctionHome = (AuctionSessionHome) context.lookup(
                    "ejb/cayenne/examples/AuctionSession");
AuctionSession auctionBean = auctionHome.create();
Collection auctions = auctionBean.getActiveAuctions();
auctionBean.remove();
%>
<html>
<head>
	<title>Cayenne EJB Facade Demo</title>
	<LINK REL="stylesheet" TYPE="text/css" href="styles.css" TITLE="default">
</head>

<body>
<h2><img src="images/cayenne_logo.gif" width=183 height=70 alt="return home" border="0" hspace="1" vspace="1"> Auction Manager</h2>
<table width="700" border="0" cellpadding="3" cellspacing="3">
<tr>
<td width="400" valign="top">
	<table width="100%" border="0">
	<tr>
		<td>Currently Active Auctions</td>
		<td>Close at</td>
	</tr>
<% 
Iterator it = auctions.iterator();
while(it.hasNext()) {
  Auction next = (Auction)it.next();
%>
	<tr>
		<td><%= next.getName() %></td>
		<td><%= next.getClosingTime() %></td>
	</tr>
<% } %>
	</table>
</td>
<td width="300" valign="top">
	<form method="POST" action="auction">
<table width="100%" border="0" bgcolor="#dddddd" cellpadding="2" cellspacing="2">
	<tr>
		<td colspan="2"><strong>Create New Auction:</strong></td>
	</tr>
	<tr>
		<td>Name:</td>
		<td><input name="name" type="text"></td>
	</tr>
	<tr>
		<td>Starts on (MM/DD/YYYY):</td>
		<td><input name="startDate" type="text"></td>
	</tr>
	<tr>
		<td>Closes on (MM/DD/YYYY):</td>
		<td><input name="endDate" type="text"></td>
	</tr>
	<tr>
		<td colspan="2" align="right"><input type="submit" value="Create Auction"/></td>
	</tr>
	</table>
	</form>
</td>
</tr>
</table>

</body>
</html>