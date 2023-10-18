package sk.iway.iwcm.system.elfinder;


import java.io.IOException;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(name = "elfinderServlet", urlPatterns = {"/admin/elfinder-connector/"})
public class ElfinderServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    protected void service(HttpServletRequest req, HttpServletResponse res)
            throws IOException {
        ConnectorController conn = new ConnectorController();
        conn.connector(req, res);
    }

}
