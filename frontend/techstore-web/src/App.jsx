import { useEffect, useState } from 'react'
import { Link, Navigate, Route, Routes, useNavigate, useSearchParams } from 'react-router-dom'
import { QRCodeSVG } from 'qrcode.react'
import api, { BACKEND } from './api'

const ROLES = {
  ADMIN: 'Administrador',
  GERENTE: 'Gerente de Tienda',
  EMPLEADO_VENTAS: 'Empleado de Ventas',
  AUDITOR: 'Auditor',
}
const PERMS = {
  ADMIN: ['Gestiona usuarios y roles', 'Configuración completa', 'Acceso total'],
  GERENTE: ['Gestiona productos de su tienda', 'Reportes de su ubicación', 'No elimina productos de otras tiendas'],
  EMPLEADO_VENTAS: ['Consulta productos', 'Actualiza stock', 'No modifica precios'],
  AUDITOR: ['Solo lectura de todos los datos', 'Genera reportes', 'Sin permisos de modificación'],
}

function AuthShell({ title, subtitle, children }) {
  return (
    <div className="auth">
      <aside className="auth-hero">
        <div className="brand"><span className="logo">🛒</span> TechStore</div>
        <div className="hero-text">
          <h1>Inventario centralizado, acceso protegido.</h1>
          <ul className="hero-list">
            <li>🔐 Autenticación con JWT y bloqueo por intentos fallidos</li>
            <li>📱 Verificación en dos pasos (Google Authenticator)</li>
            <li>👥 Permisos por rol: Admin, Gerente, Ventas y Auditor</li>
          </ul>
        </div>
        <span className="muted">Desarrollo de Soluciones en la Nube · Tecsup</span>
      </aside>
      <main className="auth-form-wrap">
        <div className="auth-card">
          <h2>{title}</h2>
          <p className="muted">{subtitle}</p>
          {children}
        </div>
      </main>
    </div>
  )
}

function PasswordField({ value, onChange, rules }) {
  const [show, setShow] = useState(false)
  const checks = [
    ['8+ caracteres', value.length >= 8],
    ['Una mayúscula', /[A-Z]/.test(value)],
    ['Un número', /\d/.test(value)],
    ['Un carácter especial', /[^A-Za-z0-9]/.test(value)],
  ]
  return (
    <>
      <label>Contraseña
        <div className="pw">
          <input name="password" type={show ? 'text' : 'password'} value={value} onChange={onChange} placeholder="••••••••" required />
          <button type="button" onClick={() => setShow(!show)}>{show ? 'Ocultar' : 'Mostrar'}</button>
        </div>
      </label>
      {rules && (
        <div className="rules">
          {checks.map(([t, ok]) => <span key={t} className={ok ? 'ok' : ''}>{ok ? '✓' : '○'} {t}</span>)}
        </div>
      )}
    </>
  )
}

function Register() {
  const [form, setForm] = useState({ fullName: '', email: '', password: '', store: '' })
  const [error, setError] = useState('')
  const [result, setResult] = useState(null)
  const change = (e) => setForm({ ...form, [e.target.name]: e.target.value })

  const submit = async (e) => {
    e.preventDefault()
    setError('')
    try {
      const { data } = await api.post('/auth/register', form)
      setResult(data)
    } catch (err) {
      setError(err.response?.data?.message || 'Error al registrar')
    }
  }

  if (result) {
    return (
      <AuthShell title="Configura tu MFA" subtitle="Escanea el código con Google Authenticator">
        <div style={{ display: 'grid', gap: 14, marginTop: 20 }}>
          <div className="alert ok">{result.message}</div>
          <div style={{ textAlign: 'center' }}>
            <div className="qr"><QRCodeSVG value={result.otpauthUri} size={180} /></div>
          </div>
          <span className="muted">¿No puedes escanear? Usa esta clave manual:</span>
          <code className="secret">{result.mfaSecret}</code>
          <Link className="btn" to="/login">Ir a iniciar sesión</Link>
        </div>
      </AuthShell>
    )
  }

  return (
    <AuthShell title="Crear cuenta" subtitle="Regístrate para acceder al sistema de inventario">
      <form onSubmit={submit}>
        <label>Nombre completo<input name="fullName" value={form.fullName} onChange={change} required /></label>
        <label>Email<input name="email" type="email" value={form.email} onChange={change} required /></label>
        <PasswordField value={form.password} onChange={change} rules />
        <label>Tienda asignada<input name="store" value={form.store} onChange={change} placeholder="Ej. Tienda Lima" required /></label>
        {error && <div className="alert error">{error}</div>}
        <button type="submit">Registrarme</button>
        <p className="muted" style={{ textAlign: 'center', margin: 0 }}>¿Ya tienes cuenta? <Link to="/login">Inicia sesión</Link></p>
      </form>
    </AuthShell>
  )
}

function Login() {
  const nav = useNavigate()
  const [form, setForm] = useState({ email: '', password: '' })
  const [error, setError] = useState('')
  const change = (e) => setForm({ ...form, [e.target.name]: e.target.value })

  const submit = async (e) => {
    e.preventDefault()
    setError('')
    try {
      const { data } = await api.post('/auth/login', form)
      sessionStorage.setItem('preAuthToken', data.preAuthToken)
      nav('/mfa')
    } catch (err) {
      setError(err.response?.data?.message || 'Error al iniciar sesión')
    }
  }

  return (
    <AuthShell title="Bienvenido de vuelta" subtitle="Inicia sesión en TechStore">
      <form onSubmit={submit}>
        <label>Email<input name="email" type="email" value={form.email} onChange={change} required /></label>
        <PasswordField value={form.password} onChange={change} />
        {error && <div className="alert error">{error}</div>}
        <button type="submit">Entrar</button>
        <div className="divider">o continúa con</div>
        <div className="social">
          <a className="btn ghost" href={`${BACKEND}/oauth2/authorization/google`}>Google</a>
          <a className="btn ghost" href={`${BACKEND}/oauth2/authorization/github`}>GitHub</a>
        </div>
        <p className="muted" style={{ textAlign: 'center', margin: 0 }}>¿No tienes cuenta? <Link to="/register">Regístrate</Link></p>
      </form>
    </AuthShell>
  )
}

function Mfa() {
  const nav = useNavigate()
  const [code, setCode] = useState('')
  const [error, setError] = useState('')

  const submit = async (e) => {
    e.preventDefault()
    setError('')
    try {
      const preAuthToken = sessionStorage.getItem('preAuthToken')
      const { data } = await api.post('/auth/mfa/verify', { preAuthToken, code })
      localStorage.setItem('token', data.token)
      sessionStorage.removeItem('preAuthToken')
      nav('/dashboard')
    } catch (err) {
      setError(err.response?.data?.message || 'Error al verificar')
      if (err.response?.status === 423) setTimeout(() => nav('/login'), 2500)
    }
  }

  return (
    <AuthShell title="Verificación en dos pasos" subtitle="Ingresa el código de 6 dígitos de tu app autenticadora">
      <form onSubmit={submit}>
        <input className="otp" inputMode="numeric" maxLength={6} placeholder="000000" value={code}
               onChange={(e) => setCode(e.target.value.replace(/\D/g, ''))} autoFocus />
        {error && <div className="alert error">{error}</div>}
        <button type="submit" disabled={code.length !== 6}>Verificar</button>
        <Link to="/login" className="muted" style={{ textAlign: 'center' }}>← Volver al login</Link>
      </form>
    </AuthShell>
  )
}

function OAuthCallback() {
  const nav = useNavigate()
  const [params] = useSearchParams()
  useEffect(() => {
    const token = params.get('token')
    if (token) { localStorage.setItem('token', token); nav('/dashboard') } else nav('/login')
  }, [])
  return <div className="auth-form-wrap"><p className="muted">Iniciando sesión…</p></div>
}

function AdminUsers({ meEmail, notify }) {
  const [list, setList] = useState([])
  const load = () => api.get('/admin/users').then((r) => setList(r.data))
  useEffect(() => { load() }, [])

  const run = async (fn, okMsg) => {
    try { await fn(); notify(okMsg, 'ok'); load() }
    catch (err) { notify(err.response?.data?.message || 'No se pudo completar', 'error') }
  }

  return (
    <div className="card">
      <h3>👥 Gestión de usuarios ({list.length})</h3>
      <div className="table-wrap">
        <table>
          <thead><tr><th>Usuario</th><th>Tienda</th><th>Origen</th><th>Rol</th><th>Estado</th><th></th></tr></thead>
          <tbody>
            {list.map((u) => (
              <tr key={u.id}>
                <td><b>{u.fullName}</b><br /><span className="muted">{u.email}</span></td>
                <td>{u.store}</td>
                <td>{u.provider}</td>
                <td>
                  <select value={u.role} onChange={(e) => run(() => api.put(`/admin/users/${u.id}`, { role: e.target.value }), 'Rol actualizado')}>
                    {Object.entries(ROLES).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
                  </select>
                </td>
                <td>{u.locked ? <span style={{ color: 'var(--warn)' }}>🔒 Bloqueado</span> : <span style={{ color: 'var(--ok)' }}>Activo</span>}</td>
                <td className="actions">
                  {u.locked && <button className="sm ghost" onClick={() => run(() => api.post(`/admin/users/${u.id}/unlock`), 'Cuenta desbloqueada')}>Desbloquear</button>}
                  {u.email !== meEmail && (
                    <button className="sm danger" onClick={() => confirm(`¿Eliminar a ${u.email}?`) && run(() => api.delete(`/admin/users/${u.id}`), 'Usuario eliminado')}>Eliminar</button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}

function Dashboard() {
  const nav = useNavigate()
  const [me, setMe] = useState(null)
  const [output, setOutput] = useState('')
  const [toast, setToast] = useState(null)

  useEffect(() => { api.get('/me').then((r) => setMe(r.data)).catch(() => nav('/login')) }, [])

  const notify = (text, type) => { setToast({ text, type }); setTimeout(() => setToast(null), 3000) }

  const reportes = async () => {
    try { const { data } = await api.get('/reportes'); setOutput(data.message) }
    catch (err) { setOutput(`Error ${err.response?.status}: tu rol no tiene acceso a los reportes`) }
  }

  const logout = () => { localStorage.removeItem('token'); nav('/login') }

  if (!me) return <div className="auth-form-wrap"><p className="muted">Cargando…</p></div>

  return (
    <>
      <header className="topbar">
        <div className="brand"><span className="logo">🛒</span> TechStore</div>
        <div className="right">
          <span className={`badge ${me.role}`}>{ROLES[me.role]}</span>
          <button className="ghost sm" onClick={logout}>Cerrar sesión</button>
        </div>
      </header>

      <div className="container">
        <div>
          <h1>Hola, {me.fullName.split(' ')[0]} 👋</h1>
          <p className="muted">Panel de control · {me.store}</p>
        </div>

        <div className="grid">
          <div className="card">
            <h3>Mi perfil</h3>
            <div style={{ display: 'flex', gap: 12, alignItems: 'center' }}>
              <div className="avatar">{me.fullName[0]}</div>
              <div><b>{me.fullName}</b><br /><span className="muted">{me.email}</span></div>
            </div>
            <p className="muted">Tienda: {me.store} · Acceso vía {me.provider}</p>
          </div>
          <div className="card">
            <h3>Mis permisos</h3>
            <ul className="perm">{PERMS[me.role].map((p) => <li key={p}>✅ {p}</li>)}</ul>
          </div>
          <div className="card">
            <h3>📊 Reportes de inventario</h3>
            <p className="muted" style={{ marginTop: 0 }}>Disponible para Admin, Gerente y Auditor.</p>
            <button onClick={reportes}>Generar reporte</button>
            {output && <pre className="out">{output}</pre>}
          </div>
        </div>

        {me.role === 'ADMIN' && <AdminUsers meEmail={me.email} notify={notify} />}
      </div>

      {toast && <div className={`alert ${toast.type} toast`}>{toast.text}</div>}
    </>
  )
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/login" />} />
      <Route path="/register" element={<Register />} />
      <Route path="/login" element={<Login />} />
      <Route path="/mfa" element={<Mfa />} />
      <Route path="/auth/callback" element={<OAuthCallback />} />
      <Route path="/dashboard" element={<Dashboard />} />
    </Routes>
  )
}