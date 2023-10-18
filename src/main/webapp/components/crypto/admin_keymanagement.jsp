<%
   sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%
   request.setAttribute("cmpName", "crypto");
   request.setAttribute("dialogTitleKey", "admin.keymanagement.title");
   request.setAttribute("dialogDescKey", "admin.keymanagement.desc");
%>
<%@ include file="/admin/layout_top_dialog.jsp" %>

<style type="text/css">
   textarea.generatedKey { width: 100%; max-width: none; height: 170px; font-size: 12px; font-family: monospace; }
   textarea.privateKey { height: 210px; margin-bottom: 8px; }
</style>
<script type="text/javascript">
   function Ok() { window.close(); }
   $(document).ready(function(){
      $("#btnOk").hide();
   });


   function getKeys()
   {
      navigator.credentials.get({
           password: true,
           mediation: 'required'
      }).then(c => {
         if (c) {
            switch (c.type) {
                case 'password':
                    console.log("mam c: ", c);
                    return c;
                    break;
            }
         }
      });
   }

   function saveKeys()
   {
       if (window.PasswordCredential)
       {
           var c = new PasswordCredential({
               id: 'iwaykey-01',
               name: document.signup.generatedKey.value,
               password: document.signup.privateKey.value
           });
           console.log("Ukladam c:", c);
           navigator.credentials.store(c);
       }
       else {
           alert("Not supported in your browser");
       }
   }

</script>

<div class="padding10" style="min-width: 800px; min-height: 400px; padding: 10px;">


   <c:if test="${generatedKeys != null}">
      <p>
         <iwcm:text key="admin.keymanagement.publicKey"/>:
         <br/>
         <textarea class="generatedKey">${generatedKeys.publicKeyEncoded}</textarea>
      </p>

      <p>
         <iwcm:text key="admin.keymanagement.privateKey"/>:
         <br>
         <textarea class="generatedKey privateKey">${generatedKeys.privateKeyEncoded}</textarea>
      </p>
   </c:if>
   <c:if test="${generatedKeys == null}">

      <div class="panel panel-info">
         <div class="panel-heading"><iwcm:text key="admin.keymanagement.setPrivateKey"/></div>
         <div class="panel-body">
            <c:if test="${keySetToSession eq true}">
               <div class="alert alert-success" role="alert"><iwcm:text key="admin.keymanagement.privateKeySuccessfullySet"/></div>
               <script type="text/javascript">
                  window.setTimeout(function() { window.close(); }, 3000);
               </script>
            </c:if>
            <c:if test="${keySetToSession eq false}">
               <div class="alert alert-danger" role="alert"><iwcm:text key="admin.keymanagement.privateKeyError"/></div>
            </c:if>

            <c:if test="${keySetToSession == null}">
               <p>
                  <iwcm:text key="admin.keymanagement.enterExistingPrivateKeyToDecryptData"/>:
                  <br/>
                  <form action="/components/crypto/admin/keymanagement/setkey" method="post">
                     <textarea name="key" class="generatedKey privateKey" placeholder="decrypt_key-..."></textarea>
                     <br/>
                     <input type="submit" value="<iwcm:text key='button.setup'/>" class="btn btn-success pull-right"/>
                  </form>
               </p>
            </c:if>
         </div>
      </div>

<%--         <div class="panel panel-info">--%>
<%--            <div class="panel-heading">Vegeneruj verejny kluc z privatneho kľúča</div>--%>
<%--            <div class="panel-body">--%>
<%--               <c:if test="${keySetToSession eq false}">--%>
<%--                  <div class="alert alert-danger" role="alert">Dešifrovací kľúč sa nepodarilo nastaviť</div>--%>
<%--               </c:if>--%>

<%--               <c:if test="${keySetToSession == null}">--%>
<%--                  <p>--%>
<%--                     Zadajte existujúci dešifrovací kľúč pre vygenerovanie public kluca:--%>
<%--                     <br/>--%>
<%--                  <form action="/components/crypto/admin/keymanagement/generate/public" method="post">--%>
<%--                     <textarea name="key" class="generatedKey privateKey"></textarea>--%>
<%--                     <br/>--%>
<%--                     <input type="submit" value="Vygenerovat" class="btn btn-success"/>--%>
<%--                  </form>--%>
<%--                  </p>--%>
<%--               </c:if>--%>
<%--            </div>--%>
<%--         </div>--%>

      <div class="panel panel-success">
         <div class="panel-heading">
            <h3 class="panel-title"><iwcm:text key="admin.keymanagement.generateNewKeys"/></h3>
         </div>
         <div class="panel-body">
            <p>
               <iwcm:text key="admin.keymanagement.generateNewKeys.desc"/>:
               <br/>
               <form action="/components/crypto/admin/keymanagement/generate" method="get">
                  <input type="submit" value="<iwcm:text key='admin.keymanagement.generate'/>" class="btn btn-success"/>
               </form>
            </p>
         </div>
      </div>
   </c:if>

</div>

<%@ include file="/admin/layout_bottom_dialog.jsp" %>