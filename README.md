# AIDLDemo
###### Server:
    运行在单独进程中的Service
    通过AIDL的方式提供
    List<Book> getBookList();
    void addBook(in Book book);
    void registerListener(IOnNewBookArrivedListener listener);
    void unregisterListener(IOnNewBookArrivedListener listener);
    等方法，Client可以跨进程调用

###### Client:
    跨进程调用Server的方法。注册Server端数据的变化监听，接收Server端的回调。