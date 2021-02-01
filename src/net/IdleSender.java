package net;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;

import java.util.ArrayList;

/**
 * Utility class for KryoNet; allows for sending of objects whenever Idle until
 * it is empty or null. Connection can be provided if and the connection ID must be matched.
 * IdleSender objects are added to a list, 'senderQue', where only the 0th element
 * is added as a listener and subsequent IdleSender's are added as listeners one at
 * a time. Ensures no overloading.
 */
public class IdleSender extends Listener {

    private static ArrayList<IdleSender> senderQue = new ArrayList<>();

    private Object parent;
    private boolean finished = false;
    private IdleOnFinish listener;
    public ArrayList<Object> objects;
    public int connection = -1;

    public IdleSender(Object parent, ArrayList<Object> objects) {
        this.parent = parent;
        this.objects = objects;
        if (senderQue.isEmpty()) initListener();
        senderQue.add(this);
    }

    public IdleSender(Object parent, ArrayList<Object> objects, int connection) {
        this.parent = parent;
        this.objects = objects;
        this.connection = connection;
        if (senderQue.isEmpty()) initListener();
        senderQue.add(this);
    }

    public IdleSender(Object parent, ArrayList<Object> objects, IdleOnFinish listener) {
        this.parent = parent;
        this.objects = objects;
        this.listener = listener;
        if (senderQue.isEmpty()) initListener();
        senderQue.add(this);
    }

    public IdleSender(Object parent, ArrayList<Object> objects, int connection, IdleOnFinish listener) {
        this.parent = parent;
        this.objects = objects;
        this.connection = connection;
        this.listener = listener;
        if (senderQue.isEmpty()) initListener();
        senderQue.add(this);
    }

    private void initListener() {
        if (parent instanceof Client) ((Client) parent).addListener(this);
        if (parent instanceof Server) ((Server) parent).addListener(this);
    }

    @Override
    public void idle(Connection connection) {
        if (finished) return;
        if (this.connection != -1) {
            if (this.connection != connection.getID()) return;
        }
        if (objects == null) {
            finish();
            return;
        }
        if (objects.isEmpty()) {
            finish();
            return;
        }
        connection.sendTCP(objects.get(0));
        objects.remove(0);
    }

    private void finish() {
        finished = true;
        if (parent instanceof Client) ((Client) parent).removeListener(this);
        if (parent instanceof Server) ((Server) parent).removeListener(this);
        if (listener != null) listener.onFinish();
        senderQue.remove(0);
        if (!senderQue.isEmpty()) senderQue.get(0).initListener();
    }


}
