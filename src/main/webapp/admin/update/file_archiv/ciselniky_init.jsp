<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %>
<%@ page import="sk.iway.iwcm.components.enumerations.EnumerationTypeDB" %>
<%@ page import="sk.iway.iwcm.components.enumerations.model.EnumerationTypeBean" %>
<%@ page import="sk.iway.iwcm.users.UserGroupsDB" %>
<%@ page import="sk.iway.iwcm.users.UserGroupDetails" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="sk.iway.iwcm.database.nestedsets.Node" %>
<%@ page import="sk.iway.iwcm.database.nestedsets.JpaNode" %>
<%@ page import="org.eclipse.persistence.jpa.JpaEntityManager" %>
<%@ page import="sk.iway.iwcm.system.jpa.JpaTools" %>
<%@ page import="sk.iway.iwcm.database.nestedsets.JpaNestedSetManager" %>
<%@ page import="sk.iway.iwcm.database.nestedsets.NestedSetManager" %>
<%@ page import="java.util.List" %>
<%@ page import="sk.iway.iwcm.components.file_archiv.*" %>
<%@ page import="java.util.Collections" %>
<%@ page import="java.util.Comparator" %>
<%@ page import="sk.iway.iwcm.components.file_archiv.mfsr.MfsrCategoryDB" %>
<%@ page import="sk.iway.iwcm.users.UsersDB" %>
<%@ page import="sk.iway.iwcm.i18n.Prop" %>
<%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %><%@
taglib prefix="html" uri="/WEB-INF/struts-html.tld" %><%@
taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><%!


    public static class OrderByCategory implements Comparator<FileArchivatorBean> {

        public int compare(FileArchivatorBean object1, FileArchivatorBean object2) {
            return Double.compare(Tools.getIntValue(object2.getCategory(),-1), Tools.getIntValue(object1.getCategory(),-1));
        }
    }
%><%
    /**
     * Nie je potrebne spustat ani pri novej instalacii, bolo to potrebne pre preklopenie funkcneho mfsr na novu stromovu strukturu.
     */
    Identity user =  UsersDB.getCurrentUser(request);
    Prop prop = Prop.getInstance(request);
    if(user == null || !user.isAdmin())
    {
        out.print(prop.getText("components.chat.error.notloggedin"));
        return;
    }


    List<FileArchivatorBean> fileArchivatorBeans = new ArrayList<FileArchivatorBean>();
    //vytvorenie stromu kategorii
    if(true)
    {
        JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();
        JpaNestedSetManager nsm = new JpaNestedSetManager(em);
    try
    {
        out.print("------ Mazem Root ----<br>");
//        JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();
//        JpaNestedSetManager nsm = new JpaNestedSetManager(em);
        em.getTransaction().begin();
        FileArchivCategoryNodeDB.getRoot().delete();
        em.getTransaction().commit();
        //em.close();
        em.clear();
        nsm.clear();
    }
    catch (Exception exc)
    {
        out.print("Root Neexistoval<br>");
        System.out.print("Root Neexistoval<br>");
        em.getTransaction().commit();
        em.clear();
        nsm.clear();
    }


    //vytvorime Roota
    try
    {
        FileArchivCategoryNodeDB.getRoot();
    }
    catch (Exception exc)
    {
        FileArchivCategoryNodeDB.createRoot();
        out.print("Vytvaram Root<br>");
    }
    //vytvorime kategorie
    if(!FileArchivCategoryNodeDB.getRoot().hasChildren())
    {
        List<UserGroupDetails> archivCategoryUserGroups = new ArrayList<UserGroupDetails>();
        for (UserGroupDetails userGroup : UserGroupsDB.getInstance().getUserGroups())
        {
            if (userGroup.getUserGroupName().startsWith("FA"))
            {
                archivCategoryUserGroups.add(userGroup);
                //out.print("Pridavam " + " id= " + userGroup.getUserGroupId() + " - " + userGroup.getUserGroupName() + " <br>");
                //Roota naplnime kategoriami
                FileArchivCategoryNodeDB.createNode(userGroup.getUserGroupName(),userGroup.getUserGroupId(),0,true);
            }
        }
    }


        for (Node<FileArchivCategoryNodeBean> nodeCCat : FileArchivCategoryNodeDB.getCategories())
        {
            fileArchivatorBeans = FileArchivatorDB.getInstance().find("category", nodeCCat.unwrap().getInt1Val());
            //fileArchivatorBeans = FileArchivatorDB.getInstance().getAll();
            Collections.sort(fileArchivatorBeans, new OrderByCategory());

            int limit = fileArchivatorBeans.size();
            //if(limit > 100)
            //limit = 0;
            boolean categoryLevel_1_exists = false;
            boolean categoryLevel_3_exists = false;
            String actualCategory = "fsdfsdsd";
            for (int i = 0; i < limit; i++)
            {
                FileArchivatorBean fab = fileArchivatorBeans.get(i);
                if (!actualCategory.equals(fab.getCategory()))
                {
                    actualCategory = fab.getCategory();
                    out.print("Vytvaram kategoriu:  " + actualCategory + "<br>");
                    System.out.println("actualCategory: " + fab.getId() + "  " + actualCategory);
                }
                if (Tools.isEmpty(fab.getFieldA()))
                {
                    System.out.println("preskakujem lebo categoria 2 je prazdna pre fab.id: " + fab.getId());
                    continue;
                }
                categoryLevel_1_exists = false;
                categoryLevel_3_exists = false;
                //out.print("Skupina level 2 "+fab.getFieldA()+" fab.id "+fab.getId()+"<br>");
                out.flush();
                for (Node<FileArchivCategoryNodeBean> node : FileArchivCategoryNodeDB.getCategories())
                {
                    //kategoria by mala existovat z predosleho kroku
                    if (node.unwrap().getInt1Val() == Tools.getIntValue(fab.getCategory(), -1))
                    {
                        if (node.hasChildren())
                        {
                            for (Node<FileArchivCategoryNodeBean> descendant : node.getChildren())
                            {
                                //out.print(fab.getFieldA()+" - "+descendant.unwrap().getCategoryName()+"<br>");
                                if (descendant.unwrap().getCategoryName().equals(fab.getFieldA()))
                                {
                                    //out.print(" OK <br>");
                                    categoryLevel_1_exists = true;
                                    continue;
                                }
                            }

                            if (!categoryLevel_1_exists)
                            {
                                //vytvarame
                                if (Tools.isNotEmpty(fab.getFieldA()))
                                {
                                    System.out.println("Neexistuje 2 skupina " + fab.getFieldA());//fab.getFieldA()
                                    int priority = -20;//MfsrCategoryDB.getPriorityForCategory(2, null, fab.getFieldA(), fab.getCategory());
                                    boolean isShow = false;//MfsrCategoryDB.canShowCategory(2, null, fab.getFieldA(), fab.getCategory());
                                    /*FileArchivCategoryNodeDB.*/
                                    FileArchivCategoryNodeDB.createSubNode(node, fab.getFieldA(), node.unwrap().getInt1Val(), priority, isShow);
                                }
                            }
                        }
                        else
                        {
                            //vytvarame
                            if (Tools.isNotEmpty(fab.getFieldA()))
                            {
                                System.out.println("Vytvorime level 2 skupina " + fab.getFieldA());
                                int priority = -20;//MfsrCategoryDB.getPriorityForCategory(2, null, fab.getFieldA(), fab.getCategory());
                                boolean isShow = false;//MfsrCategoryDB.canShowCategory(2, null, fab.getFieldA(), fab.getCategory());
                                /*FileArchivCategoryNodeDB.*/
                                FileArchivCategoryNodeDB.createSubNode(node, fab.getFieldA(), node.unwrap().getInt1Val(), priority, isShow);
                            }
                        }
                    }
                }

                //naplnime treti level
                categoryLevel_1_exists = false;
                categoryLevel_3_exists = false;
                for (Node<FileArchivCategoryNodeBean> kategoria_1 : FileArchivCategoryNodeDB.getCategories())
                {
                    // vytvorime teraz kategorie pre dalsi level 2
                    if (kategoria_1.hasChildren())
                    {
                        if (kategoria_1.unwrap().getInt1Val() != Tools.getIntValue(fab.getCategory(), -1))
                        {
                            System.out.println("nesedi kategoria preskakujeme");
                            continue;
                        }
                        //System.out.println("mame kategoria_1 id: "+kategoria_1.getId());
                        //System.out.println("a");
                        for (Node<FileArchivCategoryNodeBean> kategoria_2 : kategoria_1.getChildren())//level 1
                        {
                            //System.out.println("b");
                            if (kategoria_2.getLevel() != 2)
                            {
                                System.out.println("preskakujeme node ID : " + kategoria_2.getId() + " " + kategoria_2.unwrap().getCategoryName() + " level: " + kategoria_2.getLevel());
                                continue;
                            }
                            //System.out.println("mame kategoria_2 id: "+kategoria_2.getId());
                            if (kategoria_2.hasChildren() && (kategoria_2.unwrap().getCategoryName()).equals(fab.getFieldA()))
                            {
                                for (Node<FileArchivCategoryNodeBean> kategoria_3 : kategoria_2.getChildren()) //level 2
                                {
                                    if (Tools.isNotEmpty(fab.getFieldB()) && kategoria_3.unwrap().getCategoryName().equals(fab.getFieldB()))
                                    {
                                        categoryLevel_3_exists = true;
                                    }
                                }

                                if (!categoryLevel_3_exists)
                                {
                                    //System.out.println("mame kategoria_2 id: "+kategoria_2.getId());
                                    //vytvarame
                                    if (Tools.isNotEmpty(fab.getFieldB()))
                                    {
                                        System.out.println("Neexistuje kategoria_3: " + fab.getFieldB());
                                        int priority = -20; //MfsrCategoryDB.getPriorityForCategory(3, fab.getFieldB(), fab.getFieldA(), fab.getCategory());
                                        boolean isShow = false;//MfsrCategoryDB.canShowCategory(3, fab.getFieldB(), fab.getFieldA(), fab.getCategory());
                                        /*FileArchivCategoryNodeDB.*/
                                        FileArchivCategoryNodeDB.createSubNode(kategoria_2, fab.getFieldB(), kategoria_1.unwrap().getInt1Val(), priority, isShow);
                                    }
                                }
                            }
                            else if (!kategoria_2.hasChildren())
                            {
                                //System.out.println("mame kategoria_2 id: "+kategoria_2.getId());
                                //vytvarame
                                if (Tools.isNotEmpty(fab.getFieldB()) && kategoria_2.getLevel() == 2)
                                {
                                    //Node newNode = new Node();
                                    //Node<FileArchivCategoryNodeBean> newNode = nsm.getNode(em.find(FileArchivCategoryNodeBean.class, kategoria_2.getId()));

                                    System.out.println("Vytvorime kategoria_3 skupinu (level: " + kategoria_2.getLevel() + ") " + fab.getFieldB() + " / " + fab.getId());
                                    System.out.println("***" + kategoria_1.hasChildren() + " * " + kategoria_1.getChildren().size() + " * " + kategoria_2.hasChildren());
                                    int priority = -20;//MfsrCategoryDB.getPriorityForCategory(3, fab.getFieldB(), fab.getFieldA(), fab.getCategory());
                                    boolean isShow = false;//MfsrCategoryDB.canShowCategory(3, fab.getFieldB(), fab.getFieldA(), fab.getCategory());
                                    /*FileArchivCategoryNodeDB.*/
                                    FileArchivCategoryNodeDB.createSubNode(kategoria_2, fab.getFieldB(), kategoria_1.unwrap().getInt1Val(), priority, isShow);//priority,isShow
                                }
                            }
                        }
                    }
                }

                if (i % 100 == 0)
                    System.out.println("i: " + i);
            }
        }//for
    }//if

    //nastavime novym nodom prioritu a zobrazovat
    if(false)
    {
        int sumCat3 = 0;
        String category2Name = "";
        String separator = "&nbsp;&nbsp;&nbsp;";
        for (Node<FileArchivCategoryNodeBean> node : /*FileArchivCategoryNodeDB.*/FileArchivCategoryNodeDB.getCategories())
        {
            out.print(node.unwrap().getCategoryName() + " id: " + node.unwrap().getInt1Val() + "<br>");
            if (node.hasChildren())
            {
                Node<FileArchivCategoryNodeBean> adoptNode1 = FileArchivCategoryNodeDB.getId(node.getId());
                out.print("size: " + adoptNode1.getChildren().size() + "<br>");
                sumCat3 = 0;
                for (Node<FileArchivCategoryNodeBean> node2 : adoptNode1.getChildren())
                {
                    int priority = MfsrCategoryDB.getPriorityForCategory(2, null, node2.unwrap().getCategoryName(), node2.unwrap().getInt1Val() + "");
                    boolean isShow = MfsrCategoryDB.canShowCategory(2, null, node2.unwrap().getCategoryName(), node2.unwrap().getInt1Val() + "");
                    node2.unwrap().setPriority(priority);
                    node2.unwrap().setShow(isShow);
                    node2.unwrap().save();
                    category2Name = node2.unwrap().getCategoryName();

                    out.print(separator + node2.getLevel() + " - " + node2.unwrap().getCategoryName() + "<br>");
                    if (node2.hasChildren())
                    {
                        Node<FileArchivCategoryNodeBean> adoptNode2 = FileArchivCategoryNodeDB.getId(node2.getId());
                        out.print("size: " + adoptNode2.getChildren().size() + "<br>");
                        sumCat3 += adoptNode2.getChildren().size();
                        for (Node<FileArchivCategoryNodeBean> node3 : adoptNode2.getChildren())
                        {
                            priority = MfsrCategoryDB.getPriorityForCategory(3, node3.unwrap().getCategoryName(), category2Name, node3.unwrap().getInt1Val() + "");
                            isShow = MfsrCategoryDB.canShowCategory(3, node3.unwrap().getCategoryName(), category2Name, node3.unwrap().getInt1Val() + "");
                            node3.unwrap().setPriority(priority);
                            node3.unwrap().setShow(isShow);
                            node3.unwrap().save();
                            out.print(separator + separator + node3.getLevel() + " - " + node3.unwrap().getCategoryName() + "<br>");
                        }
                    }
                }
                out.println("***sumCat3: " + sumCat3);
            }
            out.print("<br><hr><br>");
        }
    }
    //Prepisovat kategorie v archive neideme
    /*
    fileArchivatorBeans = new ArrayList<FileArchivatorBean>();
    fileArchivatorBeans = FileArchivatorDB.getInstance().getAll();
    int id = 0;
    Node<FileArchivCategoryNodeBean> categoryFound = null;
    int celkovoSomNasielNaplnene2Kategorie = 0;
    int celkovoSomNasielNaplnene3Kategorie = 0;
    int naplneneKategorie_A_B = 0;
    int naplneneKategorie_A_a_B_uz_bolo = 0;
    int naplneneKategorie_iba_A = 0;
    int bez_kategorii = 0;
    int vyplnene_A_B_ale_nenasli = 0;
    int round = 0;
    boolean cat3found = false;
    for(FileArchivatorBean fab:fileArchivatorBeans)
    {
        if(Tools.isNotEmpty(fab.getCategory()) && Tools.isNotEmpty(fab.getFieldA()))
        {
            if (Tools.isNotEmpty(fab.getFieldB()))
            {
                categoryFound = FileArchivCategoryNodeDB.getByCategoryObj(fab.getCategory(),fab.getFieldA(),fab.getFieldB());
                if(categoryFound != null)
                {
                    out.print("<br>xxx PRED"+fab.getId()+" * "+fab.getCategory()+" * "+fab.getFieldA()+" * "+fab.getFieldB() );
                    celkovoSomNasielNaplnene3Kategorie++;
                    //out.print("<br>"+fab.getId()+" * "+fab.getCategory()+" * "+fab.getFieldA()+" * "+fab.getFieldB() );
                    naplneneKategorie_A_B++;
                    fab.setFieldB(categoryFound.getId()+"");
                    //este musime najst kategoriu a
                    categoryFound = FileArchivCategoryNodeDB.getByCategoryObj(fab.getCategory(),fab.getFieldA(), null);
                    if(categoryFound != null)
                    {
                        naplneneKategorie_A_a_B_uz_bolo++;
                        fab.setFieldA(categoryFound.getId()+"");
                    }
                    out.print("<br>xxx PO"+fab.getId()+" * "+fab.getCategory()+" * "+fab.getFieldA()+" * "+fab.getFieldB() );
                    fab.save();
                }
                else
                {
                    vyplnene_A_B_ale_nenasli++;
                    out.print("<br>!!!"+fab.getId()+" * "+fab.getCategory()+" * "+fab.getFieldA()+" * "+fab.getFieldB() );
                }
            }
            else
            {
                categoryFound = FileArchivCategoryNodeDB.getByCategoryObj(fab.getCategory(),fab.getFieldA(),null);
               if(categoryFound != null)
               {
                   celkovoSomNasielNaplnene2Kategorie++;
                   //out.print("<br>"+fab.getId()+" * "+fab.getCategory()+" * "+fab.getFieldA()+" * "+fab.getFieldB() );
                   naplneneKategorie_iba_A++;
                   fab.setFieldA(categoryFound.getId()+"");
                   fab.save();
               }
               else
               {
                   out.print("<br>!!!xxxxxxx"+fab.getId()+" * "+fab.getCategory()+" * "+fab.getFieldA()+" * "+fab.getFieldB() );
               }
            }
        }
        else
        {
            bez_kategorii++;
        }
        round++;
        if(round % 100 == 0)
        {
            System.out.println("round: " + round);
        }
    }

    out.println("<br>Spolu som nasiel naplnenych kategorii: "+(celkovoSomNasielNaplnene3Kategorie+celkovoSomNasielNaplnene2Kategorie));
    out.println("<br>1. naplneneKategorie_A_B: "+naplneneKategorie_A_B);
    out.println("<br>2. naplneneKategorie_iba_A: "+naplneneKategorie_iba_A);
    out.println("<br>3. bez_kategorii: "+bez_kategorii);
    out.println("<br>4. vyplnene_A_B_ale_nenasli: "+vyplnene_A_B_ale_nenasli);
    out.println("<br>5. spolu suborov: "+fileArchivatorBeans.size());
    out.println("<br>6. spolu 1+2+3+4: "+(naplneneKategorie_A_B+naplneneKategorie_iba_A+bez_kategorii+vyplnene_A_B_ale_nenasli));
    out.println("<br>7. naplneneKategorie_A_a_B_uz_bolo: "+naplneneKategorie_A_a_B_uz_bolo);
*/



%><br>END