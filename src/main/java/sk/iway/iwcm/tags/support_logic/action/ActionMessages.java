package sk.iway.iwcm.tags.support_logic.action;

import java.io.Serializable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class ActionMessages implements Serializable {
    /**
     * <p>Compares ActionMessageItem objects.</p>
     */
    private static final Comparator ACTION_ITEM_COMPARATOR =
        new Comparator() {
            public int compare(Object o1, Object o2) {
                return ((ActionMessageItem) o1).getOrder()
                - ((ActionMessageItem) o2).getOrder();
            }
        };

    public static final String GLOBAL_MESSAGE = "org.apache.struts.action.GLOBAL_MESSAGE";
    protected boolean accessed = false;
    protected HashMap messages = new HashMap();
    protected int iCount = 0;

    public ActionMessages() {
        super();
    }

    public ActionMessages(ActionMessages messages) {
        super();
        this.add(messages);
    }

    public void add(String property, ActionMessage message) {
        ActionMessageItem item = (ActionMessageItem) messages.get(property);
        List list;

        if (item == null) {
            list = new ArrayList();
            item = new ActionMessageItem(list, iCount++, property);

            messages.put(property, item);
        } else {
            list = item.getList();
        }

        list.add(message);
    }

    public void add(ActionMessages actionMessages) {
        if (actionMessages == null) {
            return;
        }

        // loop over properties
        Iterator props = actionMessages.properties();

        while (props.hasNext()) {
            String property = (String) props.next();

            // loop over messages for each property
            Iterator msgs = actionMessages.get(property);

            while (msgs.hasNext()) {
                ActionMessage msg = (ActionMessage) msgs.next();

                this.add(property, msg);
            }
        }
    }

    public void clear() {
        messages.clear();
    }

    public boolean isEmpty() {
        return (messages.isEmpty());
    }

    public Iterator get() {
        this.accessed = true;

        if (messages.isEmpty()) {
            return Collections.EMPTY_LIST.iterator();
        }

        ArrayList results = new ArrayList();
        ArrayList actionItems = new ArrayList();

        for (Iterator i = messages.values().iterator(); i.hasNext();) {
            actionItems.add(i.next());
        }

        // Sort ActionMessageItems based on the initial order the
        // property/key was added to ActionMessages.
        Collections.sort(actionItems, ACTION_ITEM_COMPARATOR);

        for (Iterator i = actionItems.iterator(); i.hasNext();) {
            ActionMessageItem ami = (ActionMessageItem) i.next();

            for (Iterator msgsIter = ami.getList().iterator();
                msgsIter.hasNext();) {
                results.add(msgsIter.next());
            }
        }

        return results.iterator();
    }

    public Iterator get(String property) {
        this.accessed = true;

        ActionMessageItem item = (ActionMessageItem) messages.get(property);

        if (item == null) {
            return (Collections.EMPTY_LIST.iterator());
        } else {
            return (item.getList().iterator());
        }
    }

    public boolean isAccessed() {
        return this.accessed;
    }

    public Iterator properties() {
        if (messages.isEmpty()) {
            return Collections.EMPTY_LIST.iterator();
        }

        ArrayList results = new ArrayList();
        ArrayList actionItems = new ArrayList();

        for (Iterator i = messages.values().iterator(); i.hasNext();) {
            actionItems.add(i.next());
        }

        // Sort ActionMessageItems based on the initial order the
        // property/key was added to ActionMessages.
        Collections.sort(actionItems, ACTION_ITEM_COMPARATOR);

        for (Iterator i = actionItems.iterator(); i.hasNext();) {
            ActionMessageItem ami = (ActionMessageItem) i.next();

            results.add(ami.getProperty());
        }

        return results.iterator();
    }

    public int size() {
        int total = 0;

        for (Iterator i = messages.values().iterator(); i.hasNext();) {
            ActionMessageItem ami = (ActionMessageItem) i.next();

            total += ami.getList().size();
        }

        return (total);
    }

    public int size(String property) {
        ActionMessageItem item = (ActionMessageItem) messages.get(property);

        return (item == null) ? 0 : item.getList().size();
    }

    public String toString() {
        return this.messages.toString();
    }

    protected class ActionMessageItem implements Serializable {
        protected List list = null;
        protected int iOrder = 0;
        protected String property = null;

        public ActionMessageItem(List list, int iOrder, String property) {
            this.list = list;
            this.iOrder = iOrder;
            this.property = property;
        }

        public List getList() {
            return list;
        }

        public void setList(List list) {
            this.list = list;
        }

        public int getOrder() {
            return iOrder;
        }

        public void setOrder(int iOrder) {
            this.iOrder = iOrder;
        }

        public String getProperty() {
            return property;
        }

        public void setProperty(String property) {
            this.property = property;
        }

        public String toString() {
            return this.list.toString();
        }
    }
}