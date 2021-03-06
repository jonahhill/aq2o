<%@include file="header.jsp"%>
<h3>${mdiid}</h3>


<table class="table">

	<c:forEach var="e" items="${keys}">
		<tr>
			<td>${e}</td>
			<td>${mdi[e]}</td>
		</tr>
	</c:forEach>

</table>

<!-- Subscription specific code  -->
<c:if test="${subscribed == 0}">
<div class="input-append">
	<form>
			<input type="hidden" name="mdiid" value="${mdiid}"/>
			<input class="span2" style="width: 50px;" id="appendedInputButton"
			name="subscriptionDepth" type="text" value="1" />
		<button type="submit" class="btn btn-primary" name="subscribe" value="1">Subscribe</button>
	</form>
</div>
</c:if>
<c:if test="${subscribed == 1}">
	<form>
		<input type="hidden" name="mdiid" value="${mdiid}"/>
		<input type="hidden" name="subscriptionDepth" value="0"/>
		<button type="submit" class="btn btn-primary" name="subscribe" value="0">Unsubscribe</button>
	</form>
</c:if>

<p>
	<a href="data_inspector?seriesid=${mdiid}&freq=RAW">=&gt;
		Historical data</a>.
</p>
<hr />
<%@include file="footer.jsp"%>