// IOnNewBookArrivedListener2.aidl
package aidl;

import aidl.Book;

interface IOnNewBookArrivedListener {
    void onNewBookArrived(in Book newBook);
}
