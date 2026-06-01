export default function Footer() {
  return (
    <footer className="footer">
      <div className="wrap">
        <span>© 2026 GraphCompose · MIT License</span>
        <nav className="footer-links" aria-label="Project links">
          <a href="https://github.com/demchaav/graph-compose/blob/main/CHANGELOG.md" target="_blank" rel="noopener">CHANGELOG</a>
          <a href="https://github.com/demchaav/graph-compose/tree/main/docs/adr" target="_blank" rel="noopener">ADR</a>
          <a href="https://github.com/demchaav/graph-compose/blob/main/SECURITY.md" target="_blank" rel="noopener">Security policy</a>
          <a href="https://github.com/demchaav/graph-compose/blob/main/LICENSE" target="_blank" rel="noopener">MIT</a>
        </nav>
      </div>
    </footer>
  );
}
