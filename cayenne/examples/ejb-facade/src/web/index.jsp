<%@ page
   session="false"
   isThreadSafe="true"
   isErrorPage="false"
   import="javax.naming.*,
           test.interfaces.*"
%>
<html>
<head>
	<title>Cayenne EJB Facade Demo</title>
	<LINK REL="stylesheet" TYPE="text/css" href="styles.css" TITLE="default">
</head>

<body>
<img src="images/cayenne_logo.gif" width=183 height=70 alt="return home" border="0">
<h2>Auction Manager</h2>
<table width="600" border="0" cellpadding="3" cellspacing="3">
<tr>
<td width="300" valign="top">
	<table width="100%" border="0">
	<tr>
		<td>Running Auctions</td>
		<td>Closing Date</td>
	</tr>
	<tr>
		<td>Auction 1</td>
		<td>02/02/2004</td>
	</tr>
	<tr>
		<td>Auction 1</td>
		<td>02/02/2004</td>
	</tr>
	<tr>
		<td>Auction 1</td>
		<td>02/02/2004</td>
	</tr>
	<tr>
		<td>Auction 1</td>
		<td>02/02/2004</td>
	</tr>
	<tr>
		<td>Auction 1</td>
		<td>02/02/2004</td>
	</tr>
	<tr>
		<td>Auction 1</td>
		<td>02/02/2004</td>
	</tr>
	<tr>
		<td>Auction 1</td>
		<td>02/02/2004</td>
	</tr>
	<tr>
		<td>Auction 1</td>
		<td>02/02/2004</td>
	</tr>
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