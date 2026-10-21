import { useEffect, useState } from "react";
import "./App.css";

const API = import.meta.env.VITE_API_URL || "http://localhost:8080";

function App() {
  const [page, setPage] = useState(
    localStorage.getItem("token") ? "dashboard" : "login"
  );

  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(false);

  const [documents, setDocuments] = useState([]);
  const [file, setFile] = useState(null);
  const [category, setCategory] = useState("");

  const [searchType, setSearchType] = useState("name");
  const [searchValue, setSearchValue] = useState("");

  const [adminView, setAdminView] = useState("documents");
  const [auditLogs, setAuditLogs] = useState([]);
  const [permissions, setPermissions] = useState([]);

  const token = localStorage.getItem("token");
  const currentEmail = localStorage.getItem("email");
  const currentRole = localStorage.getItem("role");

  const isAdmin = currentRole === "ADMIN";

  const authHeaders = () => ({
    Authorization: `Bearer ${localStorage.getItem("token")}`,
  });

  const clearForm = () => {
    setName("");
    setEmail("");
    setPassword("");
    setMessage("");
  };

  const goToRegister = () => {
    clearForm();
    setPage("register");
  };

  const goToLogin = () => {
    clearForm();
    setPage("login");
  };

  const handleLogin = async (e) => {
    e.preventDefault();

    if (!email || !password) {
      setMessage("Please enter email and password");
      return;
    }

    try {
      setLoading(true);
      setMessage("");

      const response = await fetch(`${API}/api/auth/login`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          email,
          password,
        }),
      });

      if (!response.ok) {
        setMessage((await response.text()) || "Login failed");
        return;
      }

      const data = await response.json();

      localStorage.setItem("token", data.token);
      localStorage.setItem("email", data.email);
      localStorage.setItem("role", data.role);

      setEmail("");
      setPassword("");
      setMessage("");
      setAdminView("documents");
      setPage("dashboard");
    } catch (error) {
      console.error(error);
      setMessage("Cannot connect to TrustVault server");
    } finally {
      setLoading(false);
    }
  };

  const handleRegister = async (e) => {
    e.preventDefault();

    if (!name || !email || !password) {
      setMessage("Please fill all fields");
      return;
    }

    if (password.length < 6) {
      setMessage("Password must contain at least 6 characters");
      return;
    }

    try {
      setLoading(true);
      setMessage("");

      const response = await fetch(`${API}/api/auth/register`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          name,
          email,
          password,
        }),
      });

      const result = await response.text();

      if (!response.ok) {
        setMessage(result || "Registration failed");
        return;
      }

      clearForm();
      setPage("login");
      setMessage("Registration successful. Please login.");
    } catch (error) {
      console.error(error);
      setMessage("Cannot connect to TrustVault server");
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = () => {
    localStorage.removeItem("token");
    localStorage.removeItem("email");
    localStorage.removeItem("role");

    setDocuments([]);
    setAuditLogs([]);
    setPermissions([]);
    setAdminView("documents");
    setMessage("");
    setPage("login");
  };

  const loadDocuments = async () => {
    if (!localStorage.getItem("token")) {
      return;
    }

    try {
      const response = await fetch(`${API}/api/documents`, {
        headers: authHeaders(),
      });

      if (response.status === 401) {
        handleLogout();
        return;
      }

      if (!response.ok) {
        setMessage("Could not load documents");
        return;
      }

      const data = await response.json();
      setDocuments(data);
    } catch (error) {
      console.error(error);
      setMessage("Could not connect to server");
    }
  };

  const loadAuditLogs = async () => {
    try {
      setLoading(true);
      setMessage("");

      const response = await fetch(`${API}/api/admin/audit`, {
        headers: authHeaders(),
      });

      if (response.status === 401) {
        handleLogout();
        return;
      }

      if (response.status === 403) {
        setMessage("Admin access required");
        return;
      }

      if (!response.ok) {
        setMessage("Could not load audit logs");
        return;
      }

      const data = await response.json();
      setAuditLogs(data);
    } catch (error) {
      console.error(error);
      setMessage("Could not load audit logs");
    } finally {
      setLoading(false);
    }
  };

  const loadPermissions = async () => {
    try {
      setLoading(true);
      setMessage("");

      const response = await fetch(`${API}/api/admin/permissions`, {
        headers: authHeaders(),
      });

      if (response.status === 401) {
        handleLogout();
        return;
      }

      if (response.status === 403) {
        setMessage("Admin access required");
        return;
      }

      if (!response.ok) {
        setMessage("Could not load permissions");
        return;
      }

      const data = await response.json();
      setPermissions(data);
    } catch (error) {
      console.error(error);
      setMessage("Could not load permissions");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (page === "dashboard" && token) {
      loadDocuments();
    }
  }, [page, token]);

  const showDocuments = () => {
    setAdminView("documents");
    setMessage("");
    loadDocuments();
  };

  const showAuditLogs = () => {
    setAdminView("audit");
    loadAuditLogs();
  };

  const showPermissions = () => {
    setAdminView("permissions");
    loadPermissions();
  };

  const handleUpload = async (e) => {
    e.preventDefault();

    if (!file) {
      setMessage("Please choose a file");
      return;
    }

    const formData = new FormData();
    formData.append("file", file);

    if (category.trim()) {
      formData.append("category", category.trim());
    }

    try {
      setLoading(true);
      setMessage("");

      const response = await fetch(`${API}/api/documents/upload`, {
        method: "POST",
        headers: authHeaders(),
        body: formData,
      });

      if (!response.ok) {
        setMessage((await response.text()) || "Upload failed");
        return;
      }

      setFile(null);
      setCategory("");
      setMessage("Document uploaded successfully!");

      const fileInput = document.getElementById("document-file");

      if (fileInput) {
        fileInput.value = "";
      }

      await loadDocuments();
    } catch (error) {
      console.error(error);
      setMessage("Upload failed");
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = async (e) => {
    e.preventDefault();

    if (!searchValue.trim()) {
      loadDocuments();
      return;
    }

    try {
      setMessage("");

      const params = new URLSearchParams();
      params.append(searchType, searchValue.trim());

      const response = await fetch(
        `${API}/api/documents?${params.toString()}`,
        {
          headers: authHeaders(),
        }
      );

      if (!response.ok) {
        setMessage("Search failed");
        return;
      }

      const data = await response.json();
      setDocuments(data);
    } catch (error) {
      console.error(error);
      setMessage("Search failed");
    }
  };

  const clearSearch = () => {
    setSearchValue("");
    setMessage("");
    loadDocuments();
  };

  const handleDownload = async (doc) => {
    try {
      const response = await fetch(
        `${API}/api/documents/${doc.id}/download`,
        {
          headers: authHeaders(),
        }
      );

      if (!response.ok) {
        setMessage((await response.text()) || "Download failed");
        return;
      }

      const blob = await response.blob();
      const url = window.URL.createObjectURL(blob);

      const link = window.document.createElement("a");
      link.href = url;
      link.download = doc.filename;

      window.document.body.appendChild(link);
      link.click();
      link.remove();

      window.URL.revokeObjectURL(url);

      setMessage("Document downloaded successfully!");
    } catch (error) {
      console.error(error);
      setMessage("Download failed");
    }
  };

  const handleEdit = async (doc) => {
    const newCategory = window.prompt(
      "Enter new category:",
      doc.category || ""
    );

    if (newCategory === null) {
      return;
    }

    if (!newCategory.trim()) {
      setMessage("Category cannot be empty");
      return;
    }

    try {
      setMessage("");

      const response = await fetch(
        `${API}/api/documents/${doc.id}`,
        {
          method: "PUT",
          headers: {
            ...authHeaders(),
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            category: newCategory.trim(),
          }),
        }
      );

      if (!response.ok) {
        setMessage((await response.text()) || "Edit failed");
        return;
      }

      setMessage("Document updated successfully!");
      await loadDocuments();
    } catch (error) {
      console.error(error);
      setMessage("Edit failed");
    }
  };

  const handleDelete = async (doc) => {
    const confirmed = window.confirm(
      `Delete "${doc.filename}" permanently?`
    );

    if (!confirmed) {
      return;
    }

    try {
      const response = await fetch(`${API}/api/documents/${doc.id}`, {
        method: "DELETE",
        headers: authHeaders(),
      });

      if (!response.ok) {
        setMessage((await response.text()) || "Delete failed");
        return;
      }

      setMessage("Document deleted successfully!");
      await loadDocuments();
    } catch (error) {
      console.error(error);
      setMessage("Delete failed");
    }
  };

  const handleShare = async (doc) => {
    const targetEmail = window.prompt(
      "Enter user email to share with:"
    );

    if (!targetEmail || !targetEmail.trim()) {
      return;
    }

    const permissionInput = window.prompt(
      "Enter permission: READ or EDIT",
      "READ"
    );

    if (permissionInput === null) {
      return;
    }

    const permission = permissionInput.trim().toUpperCase();

    if (permission !== "READ" && permission !== "EDIT") {
      setMessage("Permission must be READ or EDIT");
      return;
    }

    try {
      const response = await fetch(
        `${API}/api/documents/${doc.id}/permissions`,
        {
          method: "POST",
          headers: {
            ...authHeaders(),
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            email: targetEmail.trim(),
            permission,
          }),
        }
      );

      if (!response.ok) {
        setMessage((await response.text()) || "Share failed");
        return;
      }

      setMessage(
        `${permission} access granted to ${targetEmail.trim()}`
      );
    } catch (error) {
      console.error(error);
      setMessage("Share failed");
    }
  };

  const handleRevoke = async (doc) => {
    const targetEmail = window.prompt(
      "Enter email whose access should be revoked:"
    );

    if (!targetEmail || !targetEmail.trim()) {
      return;
    }

    try {
      const response = await fetch(
        `${API}/api/documents/${doc.id}/permissions?email=${encodeURIComponent(
          targetEmail.trim()
        )}`,
        {
          method: "DELETE",
          headers: authHeaders(),
        }
      );

      if (!response.ok) {
        setMessage((await response.text()) || "Revoke failed");
        return;
      }

      setMessage(`Access revoked from ${targetEmail.trim()}`);
    } catch (error) {
      console.error(error);
      setMessage("Revoke failed");
    }
  };

  const renderDocuments = () => {
    return (
      <>
        <section className="welcome-section">
          <h2>Document Dashboard</h2>
          <p>Upload, manage and securely share your documents.</p>
        </section>

        <section className="dashboard-card">
          <h3>Upload Document</h3>

          <form className="upload-form" onSubmit={handleUpload}>
            <input
              id="document-file"
              type="file"
              accept=".pdf,.txt,.png,.jpg,.jpeg"
              onChange={(e) =>
                setFile(e.target.files?.[0] || null)
              }
            />

            <input
              type="text"
              placeholder="Category e.g. Academic"
              value={category}
              onChange={(e) => setCategory(e.target.value)}
            />

            <button type="submit" disabled={loading}>
              {loading ? "Uploading..." : "Upload"}
            </button>
          </form>
        </section>

        <section className="dashboard-card">
          <h3>Search Documents</h3>

          <form className="search-form" onSubmit={handleSearch}>
            <select
              value={searchType}
              onChange={(e) => {
                setSearchType(e.target.value);
                setSearchValue("");
              }}
            >
              <option value="name">File Name</option>
              <option value="owner">Owner</option>
              <option value="category">Category</option>
              <option value="date">Date</option>
            </select>

            <input
              type={searchType === "date" ? "date" : "text"}
              placeholder="Search..."
              value={searchValue}
              onChange={(e) => setSearchValue(e.target.value)}
            />

            <button type="submit">Search</button>

            <button
              type="button"
              className="secondary-button"
              onClick={clearSearch}
            >
              Clear
            </button>
          </form>
        </section>

        <section className="dashboard-card">
          <div className="documents-heading">
            <h3>Accessible Documents</h3>
            <span>{documents.length} document(s)</span>
          </div>

          {documents.length === 0 ? (
            <p className="empty-text">
              No accessible documents found.
            </p>
          ) : (
            <div className="table-wrapper">
              <table>
                <thead>
                  <tr>
                    <th>File Name</th>
                    <th>Category</th>
                    <th>Owner</th>
                    <th>Permission</th>
                    <th>Date</th>
                    <th>Actions</th>
                  </tr>
                </thead>

                <tbody>
                  {documents.map((doc) => {
                    const isOwner =
                      doc.permission === "OWNER" ||
                      doc.owner?.toLowerCase() ===
                        currentEmail?.toLowerCase();

                    const canEdit =
                      isOwner || doc.permission === "EDIT";

                    return (
                      <tr key={doc.id}>
                        <td>{doc.filename}</td>
                        <td>{doc.category}</td>
                        <td>{doc.owner}</td>
                        <td>{doc.permission || "-"}</td>

                        <td>
                          {doc.createdAt
                            ? new Date(
                                doc.createdAt
                              ).toLocaleDateString()
                            : "-"}
                        </td>

                        <td>
                          <div className="action-buttons">
                            <button
                              className="small-button"
                              onClick={() => handleDownload(doc)}
                            >
                              Download
                            </button>

                            {canEdit && (
                              <button
                                className="small-button"
                                onClick={() => handleEdit(doc)}
                              >
                                Edit
                              </button>
                            )}

                            {isOwner && (
                              <>
                                <button
                                  className="small-button"
                                  onClick={() => handleShare(doc)}
                                >
                                  Share
                                </button>

                                <button
                                  className="small-button secondary-button"
                                  onClick={() => handleRevoke(doc)}
                                >
                                  Revoke
                                </button>

                                <button
                                  className="small-button delete-button"
                                  onClick={() => handleDelete(doc)}
                                >
                                  Delete
                                </button>
                              </>
                            )}
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </section>
      </>
    );
  };

  const renderAuditLogs = () => {
    return (
      <>
        <section className="welcome-section">
          <h2>Audit Logs</h2>
          <p>
            Security and document activity recorded by TrustVault.
          </p>
        </section>

        <section className="dashboard-card">
          <div className="documents-heading">
            <h3>Activity History</h3>
            <span>{auditLogs.length} event(s)</span>
          </div>

          {loading ? (
            <p className="empty-text">Loading audit logs...</p>
          ) : auditLogs.length === 0 ? (
            <p className="empty-text">
              No audit events found.
            </p>
          ) : (
            <div className="table-wrapper">
              <table>
                <thead>
                  <tr>
                    <th>Time</th>
                    <th>User</th>
                    <th>Event</th>
                    <th>Document</th>
                    <th>Status</th>
                    <th>Details</th>
                  </tr>
                </thead>

                <tbody>
                  {auditLogs.map((log) => (
                    <tr key={log.id}>
                      <td>
                        {log.timestamp
                          ? new Date(
                              log.timestamp
                            ).toLocaleString()
                          : "-"}
                      </td>
                      <td>{log.userEmail}</td>
                      <td>{log.event}</td>
                      <td>{log.documentId || "-"}</td>
                      <td>
                        {log.success ? "SUCCESS" : "REFUSED"}
                      </td>
                      <td>{log.details || "-"}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>
      </>
    );
  };

  const renderPermissions = () => {
    return (
      <>
        <section className="welcome-section">
          <h2>Permission Overview</h2>
          <p>Current document access permissions.</p>
        </section>

        <section className="dashboard-card">
          <div className="documents-heading">
            <h3>Active Permissions</h3>
            <span>{permissions.length} permission(s)</span>
          </div>

          {loading ? (
            <p className="empty-text">
              Loading permissions...
            </p>
          ) : permissions.length === 0 ? (
            <p className="empty-text">
              No active shared permissions found.
            </p>
          ) : (
            <div className="table-wrapper">
              <table>
                <thead>
                  <tr>
                    <th>Document</th>
                    <th>Owner</th>
                    <th>Shared With</th>
                    <th>Permission</th>
                  </tr>
                </thead>

                <tbody>
                  {permissions.map((permission, index) => (
                    <tr
                      key={`${permission.documentId}-${permission.sharedWith}-${index}`}
                    >
                      <td>{permission.documentName}</td>
                      <td>{permission.owner}</td>
                      <td>{permission.sharedWith}</td>
                      <td>{permission.permission}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>
      </>
    );
  };

  if (page === "dashboard") {
    return (
      <div className="dashboard">
        <header className="dashboard-header">
          <div>
            <h1>TrustVault</h1>
            <p>Secure Document Sharing</p>
          </div>

          <div className="user-area">
            {isAdmin && <span>ADMIN</span>}
            <span>{currentEmail}</span>

            <button onClick={handleLogout}>
              Logout
            </button>
          </div>
        </header>

        {isAdmin && (
          <div className="admin-navigation">
            <button
              className={
                adminView === "documents"
                  ? "admin-nav-active"
                  : ""
              }
              onClick={showDocuments}
            >
              Documents
            </button>

            <button
              className={
                adminView === "audit"
                  ? "admin-nav-active"
                  : ""
              }
              onClick={showAuditLogs}
            >
              Audit Logs
            </button>

            <button
              className={
                adminView === "permissions"
                  ? "admin-nav-active"
                  : ""
              }
              onClick={showPermissions}
            >
              Permissions
            </button>
          </div>
        )}

        <main className="dashboard-content">
          {message && (
            <div className="dashboard-message">
              {message}
            </div>
          )}

          {adminView === "audit" && isAdmin
            ? renderAuditLogs()
            : adminView === "permissions" && isAdmin
            ? renderPermissions()
            : renderDocuments()}
        </main>
      </div>
    );
  }

  return (
    <div className="app">
      <div className="login-card">
        <div className="logo">TV</div>

        <h1>TrustVault</h1>

        <p className="subtitle">
          Secure Document Sharing
        </p>

        {page === "login" ? (
          <>
            <form onSubmit={handleLogin}>
              <label>Email</label>

              <input
                type="email"
                placeholder="Enter your email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
              />

              <label>Password</label>

              <input
                type="password"
                placeholder="Enter your password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
              />

              <button type="submit" disabled={loading}>
                {loading ? "Logging in..." : "Login"}
              </button>
            </form>

            <p className="register-text">
              Don't have an account?{" "}
              <span onClick={goToRegister}>
                Register
              </span>
            </p>
          </>
        ) : (
          <>
            <form onSubmit={handleRegister}>
              <label>Full Name</label>

              <input
                type="text"
                placeholder="Enter your name"
                value={name}
                onChange={(e) => setName(e.target.value)}
              />

              <label>Email</label>

              <input
                type="email"
                placeholder="Enter your email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
              />

              <label>Password</label>

              <input
                type="password"
                placeholder="Minimum 6 characters"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
              />

              <button type="submit" disabled={loading}>
                {loading
                  ? "Creating account..."
                  : "Register"}
              </button>
            </form>

            <p className="register-text">
              Already have an account?{" "}
              <span onClick={goToLogin}>
                Login
              </span>
            </p>
          </>
        )}

        {message && (
          <p className="login-message">
            {message}
          </p>
        )}

        <p className="security-text">
          🔒 Your documents are protected with secure access control
        </p>
      </div>
    </div>
  );
}

export default App;