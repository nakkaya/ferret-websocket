(native-header "websocketpp/config/asio_no_tls.hpp"
               "websocketpp/client.hpp"
               "websocketpp/common/thread.hpp")

(defobject ws_client_o "ferret-websocket/ws_client_o.h")

(defnative connect-aux [server]
  (on "defined FERRET_STD_LIB"
      "__result = obj<ws_client_o>(server);"))

(defn connect [server]
  (let [client (connect-aux server)
        run (fn [c] "c.cast<ws_client_o>()->run();")]
    (thread #(run client))
    client))

(defn connected? [conn]
  "__result = conn.cast<ws_client_o>()->isOpen();")

(defn send [conn msg]
  "__result = conn.cast<ws_client_o>()->send(conn, msg);")
