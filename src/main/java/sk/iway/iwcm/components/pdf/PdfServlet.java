package sk.iway.iwcm.components.pdf;


import java.io.ByteArrayOutputStream;
import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.PdfTools;

@WebServlet(name = "pdfServlet", urlPatterns = {"/to.pdf/*", "/topdf/*"})
public class PdfServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException
    {
        //ak to islo napriamo tak to zhadzovalo IIS ISAPI filter!!!
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        int docId = Tools.getDocId(req);

        boolean ok = PdfTools.getPdfVersion(docId, req, baos);

        if (ok)
        {
            if(req.getParameter("renderAsRtf") != null && req.getParameter("renderAsRtf").toLowerCase().equals("true") )
                res.setContentType("html/rtf");
            else
                res.setContentType("application/pdf");

            //aby to islo aj v IE6: http://www.alagad.com/go/blog-entry/error-internet-explorer-cannot-download-filename-from-webserver
            res.setHeader("Pragma", "public");
            res.setHeader("Cache-Control", "max-age=0");

            res.setContentLength(baos.size());
            res.getOutputStream().write(baos.toByteArray());
            res.getOutputStream().flush();
            res.getOutputStream().close();
        }
        else
        {
            req.getRequestDispatcher("/404.jsp").forward(req, res);
        }

    }

}
