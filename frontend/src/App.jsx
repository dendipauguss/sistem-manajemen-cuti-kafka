import { useEffect, useMemo, useState } from "react";

const EMPTY_FORM = {
  name: "",
  nik: "",
  leaveDate: "",
  reason: "",
};

const EMPTY_USER_FORM = {
  email: "",
  password: "",
  name: "",
  nik: "",
  role: "APPLICANT",
};

function storedUser() {
  try {
    return JSON.parse(sessionStorage.getItem("leave-user") || "null");
  } catch {
    sessionStorage.removeItem("leave-user");
    sessionStorage.removeItem("leave-token");
    return null;
  }
}

async function apiRequest(path, options = {}) {
  const token = sessionStorage.getItem("leave-token");
  const response = await fetch(path, {
    ...options,
    headers: {
      ...(options.body ? { "Content-Type": "application/json" } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options.headers,
    },
  });
  const text = await response.text();
  let data;

  try {
    data = text ? JSON.parse(text) : null;
  } catch {
    data = text;
  }

  if (!response.ok) {
    if (response.status === 401) {
      sessionStorage.removeItem("leave-token");
      sessionStorage.removeItem("leave-user");
      window.dispatchEvent(new Event("auth-expired"));
    }
    const message =
      data?.detail ||
      data?.message ||
      data?.title ||
      (typeof data === "string" && data) ||
      `Permintaan gagal (HTTP ${response.status}).`;
    throw new Error(message);
  }

  return data;
}

function formatDate(value) {
  if (!value) return "-";
  return new Intl.DateTimeFormat("id-ID", {
    day: "numeric",
    month: "long",
    year: "numeric",
  }).format(new Date(`${value}T00:00:00`));
}

function formatDateTime(value) {
  if (!value) return "-";
  return new Intl.DateTimeFormat("id-ID", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

function StatusBadge({ status }) {
  const label = {
    PENDING: "Menunggu",
    APPROVED: "Disetujui",
    REJECTED: "Ditolak",
  }[status] || status;

  return <span className={`status-badge status-${status?.toLowerCase()}`}>{label}</span>;
}

function App() {
  const [user, setUser] = useState(storedUser);
  const [activePage, setActivePage] = useState(() =>
    ["OFFICER", "HR"].includes(storedUser()?.role) ? "approvals" : "apply",
  );
  const [loginForm, setLoginForm] = useState({ email: "", password: "" });
  const [loginError, setLoginError] = useState("");
  const [loginLoading, setLoginLoading] = useState(false);
  const [form, setForm] = useState(EMPTY_FORM);
  const [submitting, setSubmitting] = useState(false);
  const [submission, setSubmission] = useState(null);
  const [formError, setFormError] = useState("");
  const [myApplications, setMyApplications] = useState([]);
  const [myApplicationsLoading, setMyApplicationsLoading] = useState(false);
  const [myApplicationsError, setMyApplicationsError] = useState("");
  const [approvals, setApprovals] = useState([]);
  const [approvalsLoading, setApprovalsLoading] = useState(false);
  const [approvalsError, setApprovalsError] = useState("");
  const [filter, setFilter] = useState("ALL");
  const [decisionNotes, setDecisionNotes] = useState({});
  const [processingId, setProcessingId] = useState("");
  const [decisionError, setDecisionError] = useState("");
  const [notice, setNotice] = useState("");
  const [users, setUsers] = useState([]);
  const [usersLoading, setUsersLoading] = useState(false);
  const [usersError, setUsersError] = useState("");
  const [userForm, setUserForm] = useState(EMPTY_USER_FORM);
  const [userFormError, setUserFormError] = useState("");
  const [userFormNotice, setUserFormNotice] = useState("");
  const [creatingUser, setCreatingUser] = useState(false);
  const [passwordForm, setPasswordForm] = useState({ currentPassword: "", newPassword: "" });
  const [passwordError, setPasswordError] = useState("");
  const [passwordNotice, setPasswordNotice] = useState("");
  const [changingPassword, setChangingPassword] = useState(false);

  useEffect(() => {
    const expireSession = () => {
      setUser(null);
      setActivePage("apply");
    };
    window.addEventListener("auth-expired", expireSession);
    if (sessionStorage.getItem("leave-token")) {
      apiRequest("/api/auth/me")
        .then((profile) => {
          setUser(profile);
          sessionStorage.setItem("leave-user", JSON.stringify(profile));
        })
        .catch(() => expireSession());
    }
    return () => window.removeEventListener("auth-expired", expireSession);
  }, []);

  async function loadApprovals() {
    setApprovalsLoading(true);
    setApprovalsError("");
    try {
      const result = await apiRequest("/api/persetujuan-cuti");
      setApprovals(Array.isArray(result) ? result : []);
    } catch (error) {
      setApprovalsError(error.message);
    } finally {
      setApprovalsLoading(false);
    }
  }

  async function loadMyApplications() {
    setMyApplicationsLoading(true);
    setMyApplicationsError("");
    try {
      const result = await apiRequest("/api/pengajuan-cuti");
      setMyApplications(Array.isArray(result) ? result : []);
    } catch (error) {
      setMyApplicationsError(error.message);
    } finally {
      setMyApplicationsLoading(false);
    }
  }

  async function loadUsers() {
    setUsersLoading(true);
    setUsersError("");
    try {
      const result = await apiRequest("/api/admin/users");
      setUsers(Array.isArray(result) ? result : []);
    } catch (error) {
      setUsersError(error.message);
    } finally {
      setUsersLoading(false);
    }
  }

  useEffect(() => {
    if (user?.role === "APPLICANT" && activePage === "history") {
      loadMyApplications();
    }
    if (user && activePage === "approvals" && ["OFFICER", "HR"].includes(user.role)) {
      loadApprovals();
    }
    if (user?.role === "HR" && activePage === "users") {
      loadUsers();
    }
  }, [activePage, user]);

  useEffect(() => {
    if (user?.role === "APPLICANT") {
      setForm((current) => ({ ...current, name: user.name, nik: user.nik }));
    }
  }, [user]);

  async function signIn(event) {
    event.preventDefault();
    setLoginLoading(true);
    setLoginError("");
    try {
      const result = await apiRequest("/api/auth/login", {
        method: "POST",
        body: JSON.stringify(loginForm),
      });
      sessionStorage.setItem("leave-token", result.accessToken);
      sessionStorage.setItem("leave-user", JSON.stringify(result.user));
      setUser(result.user);
      setLoginForm({ email: "", password: "" });
      setActivePage(["OFFICER", "HR"].includes(result.user.role) ? "approvals" : "apply");
    } catch (error) {
      setLoginError(error.message);
    } finally {
      setLoginLoading(false);
    }
  }

  function signOut() {
    sessionStorage.removeItem("leave-token");
    sessionStorage.removeItem("leave-user");
    setUser(null);
    setActivePage("apply");
    setSubmission(null);
    setMyApplications([]);
    setApprovals([]);
    setUsers([]);
  }

  const counts = useMemo(
    () => ({
      pending: approvals.filter((item) => item.status === "PENDING").length,
      approved: approvals.filter((item) => item.status === "APPROVED").length,
      rejected: approvals.filter((item) => item.status === "REJECTED").length,
    }),
    [approvals],
  );
  const visibleApprovals = useMemo(
    () => filter === "ALL"
      ? approvals
      : approvals.filter((item) => item.status === filter),
    [approvals, filter],
  );

  function changePage(page) {
    setNotice("");
    setDecisionError("");
    setUserFormError("");
    setUserFormNotice("");
    if (page !== "history") setSubmission(null);
    setActivePage(page);
  }

  async function submitApplication(event) {
    event.preventDefault();
    setSubmitting(true);
    setFormError("");
    setSubmission(null);
    setNotice("");

    try {
      const result = await apiRequest("/api/pengajuan-cuti", {
        method: "POST",
        body: JSON.stringify(form),
      });
      setSubmission(result);
      setForm(EMPTY_FORM);
      setNotice("Pengajuan berhasil dikirim. Pengajuan terbaru tersedia di riwayat Anda.");
      setActivePage("history");
    } catch (error) {
      setFormError(error.message);
    } finally {
      setSubmitting(false);
    }
  }

  async function decide(requestId, status) {
    setProcessingId(requestId);
    setDecisionError("");
    setNotice("");
    try {
      await apiRequest(
        `/api/persetujuan-cuti/${encodeURIComponent(requestId)}/keputusan`,
        {
          method: "POST",
          body: JSON.stringify({
            status,
            decisionNote: decisionNotes[requestId]?.trim() || null,
          }),
        },
      );
      setNotice(
        status === "APPROVED"
          ? "Pengajuan berhasil disetujui."
          : "Pengajuan berhasil ditolak.",
      );
      setDecisionNotes((current) => ({ ...current, [requestId]: "" }));
      await loadApprovals();
    } catch (error) {
      setDecisionError(error.message);
    } finally {
      setProcessingId("");
    }
  }

  async function createUser(event) {
    event.preventDefault();
    setCreatingUser(true);
    setUserFormError("");
    setUserFormNotice("");
    try {
      const created = await apiRequest("/api/admin/users", {
        method: "POST",
        body: JSON.stringify(userForm),
      });
      setUsers((current) => [...current, created].sort((a, b) => a.email.localeCompare(b.email)));
      setUserForm(EMPTY_USER_FORM);
      setUserFormNotice(`Akun ${created.email} berhasil dibuat.`);
    } catch (error) {
      setUserFormError(error.message);
    } finally {
      setCreatingUser(false);
    }
  }

  async function changePassword(event) {
    event.preventDefault();
    setChangingPassword(true);
    setPasswordError("");
    setPasswordNotice("");
    try {
      await apiRequest("/api/auth/change-password", {
        method: "POST",
        body: JSON.stringify(passwordForm),
      });
      setPasswordForm({ currentPassword: "", newPassword: "" });
      setPasswordNotice("Kata sandi berhasil diperbarui.");
    } catch (error) {
      setPasswordError(error.message);
    } finally {
      setChangingPassword(false);
    }
  }

  if (!user) {
    return (
      <main className="auth-page">
        <section className="auth-card">
          <div className="auth-brand">
            <span className="brand-mark">SC</span>
            <span><strong>Ruang Cuti</strong><small>Portal karyawan</small></span>
          </div>
          <div className="eyebrow">SELAMAT DATANG</div>
          <h1>Masuk ke akun Anda.</h1>
          <p className="page-intro">Gunakan email kantor dan kata sandi yang diberikan HR.</p>
          <form className="auth-form" onSubmit={signIn}>
            <label className="field">
              <span>Email kantor</span>
              <input
                autoComplete="username"
                onChange={(event) => setLoginForm({ ...loginForm, email: event.target.value })}
                required
                type="email"
                value={loginForm.email}
              />
            </label>
            <label className="field">
              <span>Kata sandi</span>
              <input
                autoComplete="current-password"
                onChange={(event) => setLoginForm({ ...loginForm, password: event.target.value })}
                required
                type="password"
                value={loginForm.password}
              />
            </label>
            {loginError && <p className="alert alert-error">{loginError}</p>}
            <button className="button button-primary submit-button" disabled={loginLoading} type="submit">
              {loginLoading ? "Memeriksa akun..." : "Masuk"}
              {!loginLoading && <span aria-hidden="true">→</span>}
            </button>
          </form>
          <p className="auth-help">Belum memiliki akun? Hubungi petugas HR.</p>
        </section>
      </main>
    );
  }

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <a className="brand" href="#" onClick={() => changePage("apply")}>
          <span className="brand-mark">SC</span>
          <span>
            <strong>Ruang Cuti</strong>
            <small>Portal karyawan</small>
          </span>
        </a>

        <div className="sidebar-label">MENU UTAMA</div>
        <nav className="navigation" aria-label="Navigasi utama">
          {user.role === "APPLICANT" && (
            <button
              className={`nav-item ${activePage === "apply" ? "active" : ""}`}
              onClick={() => changePage("apply")}
            >
              <span className="nav-icon">＋</span>
              Ajukan cuti
            </button>
          )}
          {user.role === "APPLICANT" && (
            <button
              className={`nav-item ${activePage === "history" ? "active" : ""}`}
              onClick={() => changePage("history")}
            >
              <span className="nav-icon">▤</span>
              Riwayat pengajuan
            </button>
          )}
          {["OFFICER", "HR"].includes(user.role) && (
            <button
              className={`nav-item ${activePage === "approvals" ? "active" : ""}`}
              onClick={() => changePage("approvals")}
            >
              <span className="nav-icon">▤</span>
              Persetujuan
              {counts.pending > 0 && <span className="nav-count">{counts.pending}</span>}
            </button>
          )}
          {user.role === "HR" && (
            <button
              className={`nav-item ${activePage === "users" ? "active" : ""}`}
              onClick={() => changePage("users")}
            >
              <span className="nav-icon">♙</span>
              Kelola akun
            </button>
          )}
          <button
            className={`nav-item ${activePage === "password" ? "active" : ""}`}
            onClick={() => changePage("password")}
          >
            <span className="nav-icon">⌑</span>
            Ubah kata sandi
          </button>
        </nav>

        <div className="sidebar-note">
          <span className="note-icon">i</span>
          <strong>Butuh bantuan?</strong>
          <p>Hubungi tim HR jika ada pertanyaan tentang pengajuan cuti.</p>
        </div>
        <div className="sidebar-footer">
          <span className="online-dot" />
          Sistem tersedia
        </div>
      </aside>

      <main className="main-content">
        <header className="topbar">
          <div className="breadcrumb">Portal karyawan <span>/</span> {activePage === "apply" ? "Pengajuan cuti" : activePage === "history" ? "Riwayat pengajuan" : activePage === "approvals" ? "Persetujuan" : activePage === "users" ? "Kelola akun" : "Ubah kata sandi"}</div>
          <div className="profile-actions">
            <div className="profile-chip">
              <span className="profile-avatar">{user.name?.trim()?.[0]?.toUpperCase() || "U"}</span>
              <span className="profile-name">{user.name}<small>{user.role}</small></span>
            </div>
            <button className="logout-button" onClick={signOut}>Keluar</button>
          </div>
        </header>

        {activePage === "apply" && user.role === "APPLICANT" ? (
          <section className="page-content">
            <div className="welcome-row">
              <div>
                <div className="eyebrow">MANAJEMEN CUTI</div>
                <h1>Ajukan cuti dengan mudah.</h1>
                <p className="page-intro">
                  Isi detail pengajuan dan pantau proses persetujuannya di satu tempat.
                </p>
              </div>
              <div className="welcome-art" aria-hidden="true">
                <span className="art-sun" />
                <span className="art-leaf leaf-one" />
                <span className="art-leaf leaf-two" />
                <span className="art-card">✦</span>
              </div>
            </div>

            <div className="content-grid">
              <section className="panel application-panel">
                <div className="panel-heading">
                  <div>
                    <span className="step-label">FORMULIR PENGAJUAN</span>
                    <h2>Informasi cuti</h2>
                  </div>
                  <span className="heading-icon">✎</span>
                </div>
                <form className="application-form" onSubmit={submitApplication}>
                  <div className="field-row">
                    <label className="field">
                      <span>Nama lengkap</span>
                      <input
                        autoComplete="name"
                        maxLength="150"
                        name="name"
                        readOnly
                        value={form.name}
                      />
                    </label>
                    <label className="field">
                      <span>NIK</span>
                      <input
                        maxLength="30"
                        name="nik"
                        readOnly
                        value={form.nik}
                      />
                    </label>
                  </div>
                  <label className="field">
                    <span>Tanggal cuti</span>
                    <input
                      name="leaveDate"
                      onChange={(event) => setForm({ ...form, leaveDate: event.target.value })}
                      required
                      type="date"
                      value={form.leaveDate}
                    />
                    <small>Pilih tanggal cuti yang diajukan.</small>
                  </label>
                  <label className="field">
                    <span>Alasan cuti</span>
                    <textarea
                      maxLength="1000"
                      name="reason"
                      onChange={(event) => setForm({ ...form, reason: event.target.value })}
                      placeholder="Jelaskan alasan pengajuan cuti..."
                      required
                      rows="4"
                      value={form.reason}
                    />
                    <small>Maksimal 1.000 karakter.</small>
                  </label>
                  {formError && <p className="alert alert-error">{formError}</p>}
                  <button className="button button-primary submit-button" disabled={submitting} type="submit">
                    {submitting ? "Mengirim pengajuan..." : "Kirim pengajuan"}
                    {!submitting && <span aria-hidden="true">→</span>}
                  </button>
                </form>

                {submission && (
                  <div className="submission-result">
                    <div className="result-check">✓</div>
                    <div className="result-copy">
                      <strong>Pengajuan berhasil dikirim</strong>
                      <span>Status bisa dilihat lagi di menu Riwayat pengajuan.</span>
                      <code>{submission.id}</code>
                    </div>
                    <StatusBadge status={submission.status} />
                  </div>
                )}
              </section>

              <aside className="right-column">
                <section className="info-card">
                  <div className="info-illustration" aria-hidden="true">✦</div>
                  <span className="step-label">ALUR PENGAJUAN</span>
                  <h3>Proses yang transparan</h3>
                  <ol className="steps-list">
                    <li><span>1</span><div><strong>Kirim pengajuan</strong><small>Lengkapi formulir cuti.</small></div></li>
                    <li><span>2</span><div><strong>Menunggu persetujuan</strong><small>Petugas akan meninjau pengajuan.</small></div></li>
                    <li><span>3</span><div><strong>Lihat hasilnya</strong><small>Pantau status di riwayat pengajuan.</small></div></li>
                  </ol>
                </section>
              </aside>
            </div>
          </section>
        ) : activePage === "history" && user.role === "APPLICANT" ? (
          <section className="page-content">
            <div className="approval-title-row">
              <div>
                <div className="eyebrow">PENGAJUAN SAYA</div>
                <h1>Riwayat pengajuan</h1>
                <p className="page-intro">Lihat status dan detail semua pengajuan cuti Anda.</p>
              </div>
              <button
                className="button button-outline refresh-button"
                disabled={myApplicationsLoading}
                onClick={loadMyApplications}
              >
                <span aria-hidden="true">↻</span>
                {myApplicationsLoading ? "Memuat..." : "Muat ulang"}
              </button>
            </div>

            {notice && <p className="alert alert-success history-notice">{notice}</p>}
            {submission && (
              <div className="submission-result history-submission">
                <div className="result-check">✓</div>
                <div className="result-copy">
                  <strong>Pengajuan berhasil dikirim</strong>
                  <span>ID pengajuan untuk referensi:</span>
                  <code>{submission.id}</code>
                </div>
                <StatusBadge status={submission.status} />
              </div>
            )}
            {myApplicationsLoading ? (
              <div className="panel history-empty">
                <span className="loading-dot" />
                <span>Memuat riwayat pengajuan...</span>
              </div>
            ) : myApplicationsError ? (
              <div className="panel history-empty">
                <p className="alert alert-error">{myApplicationsError}</p>
                <button className="button button-outline" onClick={loadMyApplications}>
                  Coba lagi
                </button>
              </div>
            ) : myApplications.length === 0 ? (
              <div className="panel history-empty">
                <span className="empty-icon">▤</span>
                <strong>Belum ada riwayat pengajuan</strong>
                <span>Pengajuan cuti yang Anda kirim akan tampil di halaman ini.</span>
                <button className="button button-primary" onClick={() => changePage("apply")}>
                  Ajukan cuti
                </button>
              </div>
            ) : (
              <div className="history-list">
                {myApplications.map((item) => (
                  <article className="panel history-card" key={item.id}>
                    <div className="history-card-heading">
                      <div>
                        <span className="step-label">TANGGAL CUTI</span>
                        <h2>{formatDate(item.leaveDate)}</h2>
                      </div>
                      <StatusBadge status={item.status} />
                    </div>
                    <div className="history-card-meta">
                      <span>Diajukan {formatDateTime(item.createdAt)}</span>
                    </div>
                    <p className="history-reason">{item.reason}</p>
                    {item.decisionNote && (
                      <p className="history-decision">
                        <strong>Catatan keputusan:</strong> {item.decisionNote}
                      </p>
                    )}
                    <div className="history-id">
                      <span>ID pengajuan</span>
                      <code>{item.id}</code>
                    </div>
                  </article>
                ))}
              </div>
            )}
          </section>
        ) : activePage === "approvals" && ["OFFICER", "HR"].includes(user.role) ? (
          <section className="page-content">
            <div className="approval-title-row">
              <div>
                <div className="eyebrow">DASHBOARD PETUGAS</div>
                <h1>Persetujuan cuti</h1>
                <p className="page-intro">Tinjau pengajuan dan perbarui keputusan.</p>
              </div>
              <button className="button button-outline refresh-button" onClick={loadApprovals} disabled={approvalsLoading}>
                <span aria-hidden="true">↻</span>
                {approvalsLoading ? "Memuat..." : "Muat ulang"}
              </button>
            </div>

            <div className="stats-grid">
              <div className="stat-card">
                <span className="stat-icon stat-pending">◷</span>
                <div><small>Menunggu</small><strong>{counts.pending}</strong></div>
              </div>
              <div className="stat-card">
                <span className="stat-icon stat-approved">✓</span>
                <div><small>Disetujui</small><strong>{counts.approved}</strong></div>
              </div>
              <div className="stat-card">
                <span className="stat-icon stat-rejected">×</span>
                <div><small>Ditolak</small><strong>{counts.rejected}</strong></div>
              </div>
            </div>

            <section className="panel approvals-panel">
              <div className="approval-list-heading">
                <div>
                  <span className="step-label">DAFTAR PENGAJUAN</span>
                  <h2>Semua pengajuan</h2>
                </div>
                <label className="filter-control">
                  <span>Filter status</span>
                  <select value={filter} onChange={(event) => setFilter(event.target.value)}>
                    <option value="ALL">Semua status</option>
                    <option value="PENDING">Menunggu</option>
                    <option value="APPROVED">Disetujui</option>
                    <option value="REJECTED">Ditolak</option>
                  </select>
                </label>
              </div>

              {notice && <p className="alert alert-success">{notice}</p>}
              {decisionError && <p className="alert alert-error">{decisionError}</p>}
              {approvalsError && <p className="alert alert-error">{approvalsError}</p>}
              {approvalsLoading ? (
                <div className="empty-state"><span className="loading-dot" />Memuat data pengajuan...</div>
              ) : visibleApprovals.length === 0 ? (
                <div className="empty-state">
                  <span className="empty-icon">▤</span>
                  <strong>Belum ada pengajuan</strong>
                  <span>Pengajuan dengan filter ini akan tampil di sini.</span>
                </div>
              ) : (
                <div className="approval-list">
                  {visibleApprovals.map((item) => (
                    <article className="approval-card" key={item.requestId}>
                      <div className="approval-card-top">
                        <div className="employee">
                          <span className="employee-avatar">{item.name?.trim()?.[0]?.toUpperCase() || "K"}</span>
                          <div>
                            <strong>{item.name}</strong>
                            <small>NIK {item.nik}</small>
                          </div>
                        </div>
                        <StatusBadge status={item.status} />
                      </div>
                      <div className="approval-details">
                        <div><small>TANGGAL CUTI</small><strong>{formatDate(item.leaveDate)}</strong></div>
                        <div><small>DIAJUKAN</small><strong>{formatDateTime(item.createdAt)}</strong></div>
                      </div>
                      <p className="reason-text">{item.reason}</p>
                      {item.status === "PENDING" && (
                        <div className="decision-controls">
                          <input
                            aria-label={`Catatan keputusan untuk ${item.name}`}
                            maxLength="1000"
                            onChange={(event) => setDecisionNotes((current) => ({
                              ...current,
                              [item.requestId]: event.target.value,
                            }))}
                            placeholder="Catatan keputusan (opsional)"
                            value={decisionNotes[item.requestId] || ""}
                          />
                          <div className="decision-buttons">
                            <button
                              className="button button-reject"
                              disabled={processingId === item.requestId}
                              onClick={() => decide(item.requestId, "REJECTED")}
                            >
                              Tolak
                            </button>
                            <button
                              className="button button-approve"
                              disabled={processingId === item.requestId}
                              onClick={() => decide(item.requestId, "APPROVED")}
                            >
                              {processingId === item.requestId ? "Memproses..." : "Setujui"}
                            </button>
                          </div>
                        </div>
                      )}
                      {item.decisionNote && (
                        <p className="decision-note">Catatan keputusan: {item.decisionNote}</p>
                      )}
                    </article>
                  ))}
                </div>
              )}
            </section>
          </section>
        ) : activePage === "users" && user.role === "HR" ? (
          <section className="page-content">
            <div className="approval-title-row">
              <div>
                <div className="eyebrow">ADMINISTRASI</div>
                <h1>Kelola akun pengguna</h1>
                <p className="page-intro">Buat akun dan tetapkan peran sesuai tanggung jawab.</p>
              </div>
              <button className="button button-outline refresh-button" onClick={loadUsers} disabled={usersLoading}>
                <span aria-hidden="true">↻</span>
                {usersLoading ? "Memuat..." : "Muat ulang"}
              </button>
            </div>
            <div className="user-admin-grid">
              <section className="panel create-user-panel">
                <div className="panel-heading">
                  <div>
                    <span className="step-label">AKUN BARU</span>
                    <h2>Tambahkan pengguna</h2>
                  </div>
                  <span className="heading-icon">＋</span>
                </div>
                <form className="application-form" onSubmit={createUser}>
                  <label className="field">
                    <span>Nama lengkap</span>
                    <input
                      maxLength="150"
                      onChange={(event) => setUserForm({ ...userForm, name: event.target.value })}
                      required
                      value={userForm.name}
                    />
                  </label>
                  <label className="field">
                    <span>Email kantor</span>
                    <input
                      autoComplete="off"
                      maxLength="254"
                      onChange={(event) => setUserForm({ ...userForm, email: event.target.value })}
                      required
                      type="email"
                      value={userForm.email}
                    />
                  </label>
                  <label className="field">
                    <span>NIK</span>
                    <input
                      maxLength="30"
                      onChange={(event) => setUserForm({ ...userForm, nik: event.target.value })}
                      required
                      value={userForm.nik}
                    />
                  </label>
                  <label className="field">
                    <span>Kata sandi sementara</span>
                    <input
                      autoComplete="new-password"
                      maxLength="72"
                      minLength="12"
                      onChange={(event) => setUserForm({ ...userForm, password: event.target.value })}
                      required
                      type="password"
                      value={userForm.password}
                    />
                    <small>Minimal 12 karakter. Berikan secara pribadi kepada pengguna.</small>
                  </label>
                  <label className="field">
                    <span>Peran</span>
                    <select
                      onChange={(event) => setUserForm({ ...userForm, role: event.target.value })}
                      value={userForm.role}
                    >
                      <option value="APPLICANT">Pemohon</option>
                      <option value="OFFICER">Petugas persetujuan</option>
                      <option value="HR">HR / Admin</option>
                    </select>
                  </label>
                  {userFormError && <p className="alert alert-error">{userFormError}</p>}
                  {userFormNotice && <p className="alert alert-success">{userFormNotice}</p>}
                  <button className="button button-primary submit-button" disabled={creatingUser} type="submit">
                    {creatingUser ? "Membuat akun..." : "Buat akun"}
                  </button>
                </form>
              </section>

              <section className="panel users-panel">
                <div className="panel-heading">
                  <div>
                    <span className="step-label">DAFTAR AKUN</span>
                    <h2>Pengguna terdaftar</h2>
                  </div>
                  <span className="users-count">{users.length}</span>
                </div>
                {usersError && <p className="alert alert-error">{usersError}</p>}
                {usersLoading ? (
                  <div className="empty-state"><span className="loading-dot" />Memuat akun...</div>
                ) : users.length === 0 ? (
                  <div className="empty-state">
                    <span className="empty-icon">♙</span>
                    <strong>Belum ada akun</strong>
                    <span>Akun yang dibuat akan tampil di sini.</span>
                  </div>
                ) : (
                  <div className="users-list">
                    {users.map((account) => (
                      <article className="user-row" key={account.id}>
                        <span className="employee-avatar">{account.name?.trim()?.[0]?.toUpperCase() || "U"}</span>
                        <div className="user-row-copy">
                          <strong>{account.name}</strong>
                          <small>{account.email} · NIK {account.nik}</small>
                        </div>
                        <span className={`role-badge role-${account.role.toLowerCase()}`}>
                          {account.role === "APPLICANT" ? "Pemohon" : account.role === "OFFICER" ? "Petugas" : "HR"}
                        </span>
                      </article>
                    ))}
                  </div>
                )}
              </section>
            </div>
          </section>
        ) : activePage === "password" ? (
          <section className="page-content">
            <div className="approval-title-row">
              <div>
                <div className="eyebrow">KEAMANAN AKUN</div>
                <h1>Ubah kata sandi</h1>
                <p className="page-intro">Ganti kata sandi sementara dengan kata sandi pribadi yang kuat.</p>
              </div>
            </div>
            <section className="panel password-panel">
              <form className="application-form" onSubmit={changePassword}>
                <label className="field">
                  <span>Kata sandi saat ini</span>
                  <input
                    autoComplete="current-password"
                    onChange={(event) => setPasswordForm({ ...passwordForm, currentPassword: event.target.value })}
                    required
                    type="password"
                    value={passwordForm.currentPassword}
                  />
                </label>
                <label className="field">
                  <span>Kata sandi baru</span>
                  <input
                    autoComplete="new-password"
                    maxLength="72"
                    minLength="12"
                    onChange={(event) => setPasswordForm({ ...passwordForm, newPassword: event.target.value })}
                    required
                    type="password"
                    value={passwordForm.newPassword}
                  />
                  <small>Minimal 12 karakter dan maksimal 72 byte UTF-8.</small>
                </label>
                {passwordError && <p className="alert alert-error">{passwordError}</p>}
                {passwordNotice && <p className="alert alert-success">{passwordNotice}</p>}
                <button className="button button-primary submit-button" disabled={changingPassword} type="submit">
                  {changingPassword ? "Memperbarui..." : "Simpan kata sandi"}
                </button>
              </form>
            </section>
          </section>
        ) : (
          <section className="page-content">
            <p className="alert alert-error">Peran akun tidak memiliki akses ke halaman ini. Keluar lalu masuk kembali dengan akun yang sesuai.</p>
          </section>
        )}
      </main>
    </div>
  );
}

export default App;
