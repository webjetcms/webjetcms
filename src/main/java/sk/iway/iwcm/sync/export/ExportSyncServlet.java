package sk.iway.iwcm.sync.export;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(name = "exportSyncServlet", urlPatterns = {"/export.sync"})

public class ExportSyncServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException{
        String path = (String) req.getAttribute("path_filter_orig_path");
        ExportSync.getExportZip(path,req,res);
    }

}
