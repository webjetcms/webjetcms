package sk.iway.iwcm.system.datatables;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.users.UsersDB;

/**
 *  DataTablesController.java
 *
 *  DataTablesController serve as an universal entry point for displaying any data with DataTables.
 *  For example of implementation see package sk.iway.iwcm.components.enumerations
 *
 *  Title        webjet8
 *  Company      Interway a.s. (www.interway.sk)
 *  Copyright    Interway a.s (c) 2001-2018
 *  author       $Author: mhruby $
 *  version      $Revision: 1.0 $
 *  created      Date: 27.03.2018 12:00:00
 *  modified     $Date: 27.03.2018 12:0:00 $
 */

@RestController
@RequestMapping("/admin/rest/datatables/")
@PreAuthorize("@WebjetSecurityService.isAdmin()")
public class DataTablesController {

    /**
     * Index service that lookup controller from url and then display all rows from database
     * @param string class i.e. sk.iway.iwcm.components.enumerations.EnumerationDataController
     * @return
     */
    @GetMapping(path = "{string}/list", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> index(@PathVariable String string, HttpServletRequest request) {
        DataTablesWrapper wrapper = new DataTablesWrapper();
        try
        {
            @SuppressWarnings("unchecked")
            Class<? extends DataTablesInterface> c = (Class<? extends DataTablesInterface>)Class.forName(string);
            DataTablesInterface dataTables = c.getDeclaredConstructor().newInstance();
            wrapper.setData(dataTables.list(request));
        } catch(Exception e){
            sk.iway.iwcm.Logger.error(e);
        }
        return ResponseEntity.ok(wrapper);
    }

    /**
     * Save service that lookup controller from url and then save/edit row in database.
     * @param string class i.e. sk.iway.iwcm.components.enumerations.EnumerationDataController
     * @param request
     * @return
     */
    @RequestMapping(path = "{string}/save", method = {RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> save(@PathVariable String string, HttpServletRequest request) {
        DataTablesWrapper wrapper = new DataTablesWrapper();
        try
        {
            @SuppressWarnings("unchecked")
            Class<? extends DataTablesInterface> c = (Class<? extends DataTablesInterface>)Class.forName(string);
            DataTablesInterface dataTables = c.getDeclaredConstructor().newInstance();
            if (dataTables.canSave(UsersDB.getCurrentUser(request)))
            {
                List<Object> list = null;
                if ("upload".equals(request.getParameter("action")))
                {
                    dataTables.upload(request);

                    Map<String, String> filesTable = dataTables.getFiles();
                    if (filesTable!=null)
                    {
                        for (Map.Entry<String, String> entry : filesTable.entrySet())
                        {
                            String fileUrl = entry.getValue();
                            IwcmFile f = new IwcmFile(Tools.getRealPath(fileUrl));
                            if (f.exists())
                            {
                                wrapper.addUpload(fileUrl);
                            }
                        }
                    }
                }
                else
                {
                    list = dataTables.save(request, parseData(request.getParameterMap()));
                }

                if (list == null) {
                    Prop prop = Prop.getInstance(request);
                    wrapper.setFieldErrors(dataTables.getFieldErrors(), prop);
                    wrapper.setError(dataTables.getError(), prop);
                }
                wrapper.setData(list);
            } else {
                return ResponseEntity.status(403).body(null);
            }
        } catch(Exception e){
            sk.iway.iwcm.Logger.error(e);
        }
        return ResponseEntity.ok(wrapper);
    }

    /**
     * Parser for input form data from DataTables Editor.
     * Form input:
     *
     * action: create|edit|delete
     * data[0][id]: -1
     * data[0][typeName]: abc
     * data[0][type][id]: 163
     * data[1][id]: 1
     * data[1][typeName]: efg
     * data[1][type][id]: 164
     *
     * converts to
     * mainMap.get(0) => testMap
     *
     * testMap.get("action") => create|edit|delete
     * testMap.get("id") => -1
     * testMap.get("typeName) => abc
     * testMap.get("typeid") => 163
     *
     * @param parameterMap
     * @return
     */

    private Map<Integer, Map<String, String>> parseData (Map<String, String[]> parameterMap) {
        Map<Integer, Map<String, String>> tempMap = new HashMap<>();
        if (parameterMap.get("action") != null) {
            String action = parameterMap.get("action")[0];
            for (Map.Entry<String, String[]> item : parameterMap.entrySet())
            {
                //preskakujeme __sfu=1&__setf=1&__token=xxxx
                if (item.getKey().startsWith("__")) continue;

                if (!item.getKey().equals("action")) {
                    String[] parts = item.getKey().substring(4).replace("\\[", "").split("\\]");
                    Map<String,String> map;
                    if (tempMap.containsKey(Tools.getIntValue(parts[0],-1))) {
                        map = tempMap.get(Tools.getIntValue(parts[0],-1));
                    } else {
                        map = new HashMap<>();
                        map.put("action", action);
                        tempMap.put(Tools.getIntValue(parts[0],-1),map);
                    }
                    if (parts.length > 2)
                        map.put(parts[parts.length-2] + parts[parts.length-1], String.join(",", item.getValue()));
                    else
                        map.put(parts[parts.length-1], String.join(",", item.getValue()));
                }
            }
        }
        return tempMap;
    }
}
