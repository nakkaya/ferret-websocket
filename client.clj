(native-header "websocketpp/config/asio_no_tls.hpp"
               "websocketpp/client.hpp"
               "websocketpp/common/thread.hpp")

(defobject WSClient
  (data "typedef websocketpp::client<websocketpp::config::asio> client;"
        "typedef websocketpp::lib::lock_guard<websocketpp::lib::mutex> scoped_lock;"
        "client m_client;"
        "websocketpp::connection_hdl m_hdl;"
        "websocketpp::lib::mutex m_lock;"
        "bool m_open;"
        "bool m_done;"
        "var url;")
  (new ("var u"
        "url = u;
         m_open = false;
         m_done = false;
         m_client.clear_access_channels(websocketpp::log::alevel::all);
         m_client.clear_error_channels(websocketpp::log::alevel::all);
         m_client.init_asio();
         using websocketpp::lib::placeholders::_1;
         using websocketpp::lib::bind;
         m_client.set_open_handler(bind(&WSClient::on_open,this,_1));
         m_client.set_close_handler(bind(&WSClient::on_close,this,_1));
         m_client.set_fail_handler(bind(&WSClient::on_fail,this,_1));"))
  (equals "return obj<boolean>(this == o.cast<WSClient>());")
  (stream_console "runtime::print(\"WSClient\"); return nil();")
  (fns
   ("void on_open" "websocketpp::connection_hdl hdl"
    "scoped_lock guard(m_lock);
     m_open = true;")

   ("void on_close" "websocketpp::connection_hdl hdl"
    "scoped_lock guard(m_lock);
     m_done = true;")

   ("void on_fail" "websocketpp::connection_hdl hdl"
    "scoped_lock guard(m_lock);
     m_done = true;")

   ("void run" ""
    "websocketpp::lib::error_code ec;
     std::string server = string::to<std::string>(url);
     client::connection_ptr con = m_client.get_connection(server, ec);
     std::string origin = \"http\" + server.substr(2);
     con->replace_header(\"Origin\",origin);
     m_hdl = con->get_handle();
     m_client.connect(con);
     m_client.run();")

   ("var isOpen" ""
    "scoped_lock guard(m_lock);
     return obj<boolean>(m_open && !m_done);")

   ("var send" "var conn, var msg"
    "scoped_lock guard(m_lock);
     websocketpp::lib::error_code ec;
     std::stringstream val;
     val << string::to<std::string>(msg);
     m_client.send(m_hdl, val.str(), websocketpp::frame::opcode::text, ec);
     if (ec){
       std::cout << \"Send Error: \" << ec.message() << std::endl;
       return nil();
     }else
       return msg;")))

(defnative connect-aux [server]
  (on "defined FERRET_STD_LIB"
      "__result = obj<WSClient>(server);"))

(defn connect [server]
  (let [client (connect-aux server)
        run (fn [c] "c.cast<WSClient>()->run();")]
    (thread #(run client))
    client))

(defn connected? [conn]
  "__result = conn.cast<WSClient>()->isOpen();")

(defn send [conn msg]
  "__result = conn.cast<WSClient>()->send(conn, msg);")
