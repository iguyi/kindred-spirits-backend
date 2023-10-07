package com.guyi.kindredspirits.util;

import com.guyi.kindredspirits.model.domain.User;

import java.util.ArrayList;
import java.util.List;

public class LinkedUtil {

    /*public static void main(String[] args) {
        User user2 = new User();
        user2.setId(2L);
        User user3 = new User();
        user3.setId(3L);
        User user4 = new User();
        user4.setId(4L);
        User user5 = new User();
        user5.setId(5L);
        User user6 = new User();
        user6.setId(6L);
        User user7 = new User();
        user7.setId(7L);

        List<User> userList = Arrays.asList(user2, user3, user4, user5, user6, user7);
        List<Integer> integerList =Arrays.asList(4, 2, 2, 0, 2, 5);

        LinkedUtil linkedUtil = new LinkedUtil(6);
        for (int i = 0; i < 6; i++) {
            linkedUtil.add(userList.get(i), integerList.get(i));
        }
        for (User user : linkedUtil.getList()) {
            System.out.println(user.getId());
        }
    }*/

    /**
     * 维护一个 size 的有序双向链表
     */
    private final long size;

    /**
     * 记录当前链表的大小
     */
    private long counter;

    /**
     * 头节点
     */
    private Node head;

    /**
     * 尾节点
     */
    private Node tail;

    public LinkedUtil(long size) {
        this.size = size;
    }

    public void add(User user, int distance) {
        Node newNode = new Node(user, distance);
        // 链表中没有元素
        if (counter == 0) {
            head = newNode;
            tail = newNode;
            counter++;
            return;
        }
        Node current = head;
        while (current != null) {
            // distance 越小, 离 head 越近
            if (current.distance > newNode.distance) {  // 插队
                Node currentLast = current.last;
                if (currentLast == null) {  // 插在头节点前面
                    newNode.next = head;
                    head.last = newNode;
                    head = newNode;
                    counter++;
                    if (counter > size) {
                        tail = tail.last;
                        tail.next = null;
                        counter--;
                    }
                    return;
                }
                currentLast.next = newNode;
                newNode.last = currentLast;
                newNode.next = current;
                current.last = newNode;
                counter++;
                if (counter > size) {
                    tail = tail.last;
                    tail.next = null;
                    counter--;
                }
                return;
            }
            current = current.next;
        }
        if (counter < size) {
            newNode.last = tail;
            tail.next = newNode;
            tail = newNode;
            counter++;
        }
    }

    public List<User> getList() {
        List<User> userList = new ArrayList<>();
        Node currentNode = head;
        while (currentNode != null) {
            userList.add(currentNode.user);
            currentNode = currentNode.next;
        }
        return userList;
    }

    private static class Node {
        /**
         * 用户对象
         */
        private final User user;

        /**
         * 相似度
         */
        private final int distance;

        /**
         * 上一个节点
         */
        private Node last;

        /**
         * 下一个节点
         */
        private Node next;

        public Node(User user, int distance) {
            this.user = user;
            this.distance = distance;
        }
    }

}
