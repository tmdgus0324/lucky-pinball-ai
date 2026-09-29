import { useState, type FormEvent } from 'react';
import { api } from '../api/client';

interface AdminLoginProps {
  onSuccess: () => void;
}

export function AdminLogin({ onSuccess }: AdminLoginProps) {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [status, setStatus] = useState<{ text: string; error: boolean }>({ text: '', error: false });
  const [loggingIn, setLoggingIn] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setLoggingIn(true);
    setStatus({ text: '', error: false });
    try {
      await api.adminLogin(username, password);
      onSuccess();
    } catch (error) {
      setStatus({ text: (error as Error).message, error: true });
    } finally {
      setLoggingIn(false);
    }
  }

  return (
    <section className="panel" style={{ maxWidth: 360, margin: '0 auto' }}>
      <h2>🛠 관리자 로그인</h2>
      <p className="desc">
        관리자 화면은 로그인이 필요합니다.
        <br />
        관리자 계정 정보는 지원서(포트폴리오)에 안내되어 있습니다.
      </p>
      <form className="reg-form" onSubmit={handleSubmit} style={{ flexDirection: 'column', alignItems: 'stretch' }}>
        <input
          type="text"
          name="username"
          placeholder="아이디"
          autoComplete="username"
          required
          value={username}
          onChange={(e) => setUsername(e.target.value)}
        />
        <input
          type="password"
          name="password"
          placeholder="비밀번호"
          autoComplete="current-password"
          required
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />
        <button type="submit" disabled={loggingIn}>
          로그인
        </button>
      </form>
      <p className={`status-text${status.error ? ' error' : ''}`}>{status.text}</p>
    </section>
  );
}
