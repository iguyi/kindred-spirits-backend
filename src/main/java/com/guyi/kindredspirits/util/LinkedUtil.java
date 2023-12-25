package com.guyi.kindredspirits.util;

import com.guyi.kindredspirits.model.domain.User;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 孤诣
 */
public class LinkedUtil {

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

    public void add(User user, double similarity) {
        Node newNode = new Node(user, similarity);
        // 链表中没有元素
        if (counter == 0) {
            head = newNode;
            tail = newNode;
            counter++;
            return;
        }
        Node current = head;
        while (current != null) {
            // similarity 越小, 离 head 越近
            if (current.similarity > newNode.similarity) {
                // 插队
                Node currentLast = current.last;
                if (currentLast == null) {
                    // 插在头节点前面
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
        private final double similarity;

        /**
         * 上一个节点
         */
        private Node last;

        /**
         * 下一个节点
         */
        private Node next;

        public Node(User user, double similarity) {
            this.user = user;
            this.similarity = similarity;
        }
    }

}
