#RSocket Basics
**RSocket** is a binary protocol for use on byte stream transports such as TCP, WebSockets, and Aeron.

It enables the following symmetric interaction models via async message passing over a single connection:

* **request/response** (stream of 1)
* **request/stream** (finite stream of many)
* **fire-and-forget** (no response)
* **channel** (bi-directional streams)

It supports session resumption, to allow resuming long-lived streams across different transport connections. This is particularly useful for mobile⬄server communication when network connections drop, switch, and reconnect frequently.

## Terminology
- **Frame**: A single message containing a request, response, or protocol processing.
- **Fragment**: A portion of an application message that has been partitioned for inclusion in a Frame. See Fragmentation and Reassembly.
- **Transport**: Protocol used to carry RSocket protocol. One of WebSockets, TCP, or Aeron. The transport MUST provide capabilities mentioned in the transport protocol section.
- **Stream**: Unit of operation (request/response, etc.). See Motivations.
- **Request**: A stream request. May be one of four types. As well as request for more items or cancellation of previous request.
- **Payload**: A stream message (upstream or downstream). Contains data associated with a stream created by a previous request. In Reactive Streams and Rx this is the 'onNext' event.
- **Complete**: Terminal event sent on a stream to signal successful completion. In Reactive Streams and Rx this is the 'onComplete' event.
A frame (**PAYLOAD** or **REQUEST_CHANNEL**) with the Complete bit set is sometimes referred to as COMPLETE in this document when reference to the frame is semantically about the Complete bit/event.
- **Client**: The side initiating a connection.
- **Server**: The side accepting connections from clients.
- **Connection**: The instance of a transport session between client and server.
- **Requester**: The side sending a request. A connection has at most 2 Requesters. One in each direction.
- **Responder**: The side receiving a request. A connection has at most 2 Responders. One in each direction.

## When to use RSocket

_BareHTTP_ just doesn't cut it, especially in the modern world where software architecture leans heavily towards microservices.

**Microservices** need to communicate potentially with a myriad of other microservices, in a tangled and twisted dance that doesn’t always go along with the core principles upon which HTTP has been built: sending text over the wire, in a request ⇄ response fashion.

It is often required that microservices send out events in a fire-and-forget manner (brokers and advanced messaging protocols help with this but at the cost of adding significant complexity to infrastructure and applications relying on them), or request some data and hold onto the connection expecting a stream of data coming through as a response, over time.

HTTP is not an efficient solution in either of these scenarios, whereas a transport protocol that’s been built specifically for computers talking to other computers asynchronously and with high-performance in mind, such as WebSocket, seems to be a very good fit.

_RSocket_ provides all the advantages of choosing the best transport protocol for the task, and builds things like Reactive Streams semantics, backpressure management, load-balancing hints and resumability on top of it! Great stuff!