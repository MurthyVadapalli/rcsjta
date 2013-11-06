/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: C:\\Dev\\workspace\\RCS_API\\rcsjta\\core\\src\\org\\gsma\\joyn\\IJoynServiceRegistrationListener.aidl
 */
package org.gsma.joyn;
/**
 * Joyn service registration events listener
 */
public interface IJoynServiceRegistrationListener extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements org.gsma.joyn.IJoynServiceRegistrationListener
{
private static final java.lang.String DESCRIPTOR = "org.gsma.joyn.IJoynServiceRegistrationListener";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an org.gsma.joyn.IJoynServiceRegistrationListener interface,
 * generating a proxy if needed.
 */
public static org.gsma.joyn.IJoynServiceRegistrationListener asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof org.gsma.joyn.IJoynServiceRegistrationListener))) {
return ((org.gsma.joyn.IJoynServiceRegistrationListener)iin);
}
return new org.gsma.joyn.IJoynServiceRegistrationListener.Stub.Proxy(obj);
}
@Override public android.os.IBinder asBinder()
{
return this;
}
@Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
{
switch (code)
{
case INTERFACE_TRANSACTION:
{
reply.writeString(DESCRIPTOR);
return true;
}
case TRANSACTION_onServiceRegistered:
{
data.enforceInterface(DESCRIPTOR);
this.onServiceRegistered();
reply.writeNoException();
return true;
}
case TRANSACTION_onServiceUnregistered:
{
data.enforceInterface(DESCRIPTOR);
this.onServiceUnregistered();
reply.writeNoException();
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements org.gsma.joyn.IJoynServiceRegistrationListener
{
private android.os.IBinder mRemote;
Proxy(android.os.IBinder remote)
{
mRemote = remote;
}
@Override public android.os.IBinder asBinder()
{
return mRemote;
}
public java.lang.String getInterfaceDescriptor()
{
return DESCRIPTOR;
}
@Override public void onServiceRegistered() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_onServiceRegistered, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void onServiceUnregistered() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_onServiceUnregistered, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
}
static final int TRANSACTION_onServiceRegistered = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_onServiceUnregistered = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
}
public void onServiceRegistered() throws android.os.RemoteException;
public void onServiceUnregistered() throws android.os.RemoteException;
}
