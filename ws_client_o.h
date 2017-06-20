class ws_client_o : public object_t {
  
  typedef websocketpp::client<websocketpp::config::asio> client;
  typedef websocketpp::lib::lock_guard<websocketpp::lib::mutex> scoped_lock;
  
  client m_client;
  websocketpp::connection_hdl m_hdl;
  websocketpp::lib::mutex m_lock;
  bool m_open;
  bool m_done;
  var url;

 public:
  ws_client_o(var u) {
    url = u;
    m_open = false;
    m_done = false;
    m_client.clear_access_channels(websocketpp::log::alevel::all);
    m_client.clear_error_channels(websocketpp::log::alevel::all);
    m_client.init_asio();
    using websocketpp::lib::placeholders::_1;
    using websocketpp::lib::bind;
    m_client.set_open_handler(bind(&ws_client_o::on_open, this, _1));
    m_client.set_close_handler(bind(&ws_client_o::on_close, this, _1));
    m_client.set_fail_handler(bind(&ws_client_o::on_fail, this, _1));
  }

  size_t type() const { return runtime::type::ws_client_o; }

  bool equals(var const & o) const { return obj<boolean>(this == o.cast<ws_client_o>()); }

#if !defined(FERRET_DISABLE_STD_OUT)
  void stream_console() const {
    runtime::print("ws_client_o");
  }
#endif

  void on_open(websocketpp::connection_hdl hdl) {
    scoped_lock guard(m_lock);
    m_open = true;
  }
  
  void on_close(websocketpp::connection_hdl hdl) {
    scoped_lock guard(m_lock);
    m_done = true;
  }
  
  void on_fail(websocketpp::connection_hdl hdl) {
    scoped_lock guard(m_lock);
    m_done = true;
  }
  
  void run() {
    websocketpp::lib::error_code ec;
    std::string server = string::to<std::string>(url);
    client::connection_ptr con = m_client.get_connection(server, ec);
    std::string origin = "http" + server.substr(2);
    con->replace_header("Origin", origin);
    m_hdl = con->get_handle();
    m_client.connect(con);
    m_client.run();
  }
  
  var isOpen() {
    scoped_lock guard(m_lock);
    return obj<boolean>(m_open && !m_done);
  }

  var send(var conn, var msg) {
    scoped_lock guard(m_lock);
    websocketpp::lib::error_code ec;
    std::stringstream val;
    val << string::to<std::string>(msg);
    m_client.send(m_hdl, val.str(), websocketpp::frame::opcode::text, ec);
    if (ec) {
      std::cout << "Send Error: " << ec.message() << std::endl;
      return nil();
    } else
      return msg;
  }
};
