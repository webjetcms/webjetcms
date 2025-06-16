package sk.iway.iwcm.tags.support.action;

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
    private static final Comparator<ActionMessageItem> ACTION_ITEM_COMPARATOR =
        new Comparator<ActionMessageItem>() {
            public int compare(ActionMessageItem o1, ActionMessageItem o2) {
                return o1.getOrder() - o2.getOrder();
            }
        };

    public static final String GLOBAL_MESSAGE = "org.apache.struts.action.GLOBAL_MESSAGE";
    protected boolean accessed = false;
    protected HashMap<String, ActionMessageItem> messages = new HashMap<>();
    protected int iCount = 0;

    public ActionMessages() {
        super();
    }

    public ActionMessages(ActionMessages messages) {
        super();
        this.add(messages);
    }

    public void add(String property, ActionMessage message) {
        ActionMessageItem item = messages.get(property);
        List<ActionMessage> list;

        if (item == null) {
            list = new ArrayList<>();
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
        Iterator<String> props = actionMessages.properties();

        while (props.hasNext()) {
            String property = props.next();

            // loop over messages for each property
            Iterator<ActionMessage> msgs = actionMessages.get(property);

            while (msgs.hasNext()) {
                ActionMessage msg =  msgs.next();

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

    public Iterator<ActionMessage> get() {
        this.accessed = true;

        if (messages.isEmpty()) {
            return Collections.<ActionMessage>emptyList().iterator();
        }

        ArrayList<ActionMessage> results = new ArrayList<>();
        ArrayList<ActionMessageItem> actionItems = new ArrayList<>();

        for (Iterator<ActionMessageItem> i = messages.values().iterator(); i.hasNext();) {
            actionItems.add(i.next());
        }

        // Sort ActionMessageItems based on the initial order the
        // property/key was added to ActionMessages.
        Collections.sort(actionItems, ACTION_ITEM_COMPARATOR);

        for (Iterator<ActionMessageItem> i = actionItems.iterator(); i.hasNext();) {
            ActionMessageItem ami = i.next();

            for (Iterator<ActionMessage> msgsIter = ami.getList().iterator();
                msgsIter.hasNext();) {
                results.add(msgsIter.next());
            }
        }

        return results.iterator();
    }

    public Iterator<ActionMessage> get(String property) {
        this.accessed = true;

        ActionMessageItem item = messages.get(property);

        if (item == null) {
            return Collections.<ActionMessage>emptyList().iterator();
        } else {
            return (item.getList().iterator());
        }
    }

    public boolean isAccessed() {
        return this.accessed;
    }

    public Iterator<String> properties() {
        if (messages.isEmpty()) {
            return Collections.<String>emptyList().iterator();
        }

        ArrayList<String> results = new ArrayList<>();
        ArrayList<ActionMessageItem> actionItems = new ArrayList<>();

        for (Iterator<ActionMessageItem> i = messages.values().iterator(); i.hasNext();) {
            actionItems.add(i.next());
        }

        // Sort ActionMessageItems based on the initial order the
        // property/key was added to ActionMessages.
        Collections.sort(actionItems, ACTION_ITEM_COMPARATOR);

        for (Iterator<ActionMessageItem> i = actionItems.iterator(); i.hasNext();) {
            ActionMessageItem ami = i.next();

            results.add(ami.getProperty());
        }

        return results.iterator();
    }

    public int size() {
        int total = 0;

        for (Iterator<ActionMessageItem> i = messages.values().iterator(); i.hasNext();) {
            ActionMessageItem ami = i.next();

            total += ami.getList().size();
        }

        return (total);
    }

    public int size(String property) {
        ActionMessageItem item = messages.get(property);

        return (item == null) ? 0 : item.getList().size();
    }

    public String toString() {
        return this.messages.toString();
    }

    protected class ActionMessageItem implements Serializable {
        protected List<ActionMessage> list = null;
        protected int iOrder = 0;
        protected String property = null;

        public ActionMessageItem(List<ActionMessage> list, int iOrder, String property) {
            this.list = list;
            this.iOrder = iOrder;
            this.property = property;
        }

        public List<ActionMessage> getList() {
            return list;
        }

        public void setList(List<ActionMessage> list) {
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