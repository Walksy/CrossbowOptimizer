<h1>Crossbow Optimizer!</h1>

This mod handles crossbow reloading completely on the client, reducing the effect of ping and ensuring the server doesn't interrupt the reloading process.

---
**The Problem**

When reloading, the client must wait for the use-item packet to reach the server, wait for the server to process the reload, and finally wait for the server to sync the charged crossbow state back to the client. This same round-trip latency affects shooting too where you can't start reloading again until the server confirms your crossbow has fired and updates it to an empty state, which it has to send back to the client causing even more delays. On top of this, high ping can cause the server to desync and cancel your reloads randomly.

Ping shouldn't effect the speed or consistency in which you reload.

**The Solution**

This mod fixes this by moving both reloading and shooting to the client. It empties the crossbow as soon as you shoot, letting you start your next reload immediately without waiting on your ping. By filtering out invalid server updates (normally causes by high ping, packet loss etc), the mod prevents the server from cancelling your active reloads. The server still handles the arrows being fired from the crossbow.

**Demonstration Video**

https://youtu.be/T9G1JE7esN4

---
**Credits**
- Mod: Walksy
- Icon Design: SakuraFX / Luna

