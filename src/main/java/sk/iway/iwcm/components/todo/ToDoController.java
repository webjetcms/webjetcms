package sk.iway.iwcm.components.todo;

import java.util.Date;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Tools;

/**
 * ToDoController.java
 *
 * Class ToDoController is used for
 *
 *
 * Title        webjet8
 * Company      Interway a.s. (www.interway.sk)
 * Copyright    Interway a.s. (c) 2001-2018
 * @author      $Author: mhruby $
 * @version     $Revision: 1.0 $
 * created      16.8.2018 12:12
 * modified     16.8.2018 12:12
 */

@RestController
@RequestMapping("/rest/private/todo/")
@PreAuthorize("@WebjetSecurityService.isLogged()")
public class ToDoController {

    @GetMapping(path = "list", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ToDoBean>> list(HttpServletRequest request) {
        return ResponseEntity.ok(new ToDoDB().getToDo(Tools.getUserId(request)));
    }

    @GetMapping(path = "resolve/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public  ResponseEntity<List<ToDoBean>> resolve(@PathVariable String id, HttpServletRequest request) {
        ToDoDB toDoDB = new ToDoDB();
        try {
            ToDoBean toDoBean = toDoDB.findFirstByToDoId(Tools.getIntValue(id,-1));
            if (toDoBean != null && Tools.getUserId(request) == toDoBean.getUserId()) {
                toDoBean.setIsResolved(!toDoBean.getResolved());
                toDoBean.setModifDate(new Date());
                if (ToDoDB.saveToDo(toDoBean))
                    return ResponseEntity.ok(null);
                return ResponseEntity.status(500).body(null);
            }
        } catch (Exception e) {
            sk.iway.iwcm.Logger.error(e);
            return ResponseEntity.status(500).body(null);
        }
        return ResponseEntity.status(404).body(null);
    }

    @GetMapping(path = "delete/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public  ResponseEntity<List<ToDoBean>> delete(@PathVariable String id, HttpServletRequest request) {
        ToDoDB toDoDB = new ToDoDB();
        try {
            ToDoBean toDoBean = toDoDB.findFirstByToDoId(Tools.getIntValue(id,-1));
            if (toDoBean != null && Tools.getUserId(request) == toDoBean.getUserId()) {
                if (toDoDB.remove(toDoBean))
                    return ResponseEntity.ok(null);
                return ResponseEntity.status(500).body(null);
            }
        } catch (Exception e) {
            sk.iway.iwcm.Logger.error(e);
            return ResponseEntity.status(500).body(null);
        }
        return ResponseEntity.status(404).body(null);
    }
}