package sk.iway.iwcm.system.elfinder;


import java.io.IOException;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.users.UsersDB;

@WebServlet(name = "elfinderServlet", urlPatterns = {"/admin/elfinder-connector/"})
public class ElfinderServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    protected void service(HttpServletRequest req, HttpServletResponse res)
            throws IOException {

        Identity user = UsersDB.getCurrentUser(req);
        if (user == null || user.isAdmin()==false) {
            res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "User not logged in");
            return;
        }

        ConnectorController conn = new ConnectorController();
        conn.connector(req, res);
    }

}
