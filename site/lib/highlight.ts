/* Tiny Java highlighter for static <pre> blocks (hero, pipeline,
   engineering, gallery modal). The live editor uses Monaco instead. */
const KW = /\b(public|private|class|void|static|new|return|import|implements|extends|throws|final|var|interface|package)\b/g;
const TY = /\b([A-Z][A-Za-z0-9_]+)\b/g;

function esc(s: string) {
  return s.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
}

export function highlightJava(code: string): string {
  let out = esc(code);
  out = out.replace(/("(?:[^"\\]|\\.)*")/g, "\u0001$1\u0002"); // protect strings
  out = out.replace(/(\/\/[^\n]*)/g, "\u0003$1\u0004"); // protect comments
  out = out.replace(KW, '<span class="tk-key">$1</span>');
  out = out.replace(TY, '<span class="tk-type">$1</span>');
  out = out.replace(/\.([a-z][A-Za-z0-9_]*)\(/g, '.<span class="tk-mth">$1</span>(');
  out = out.replace(/\b(\d[\d_.]*)\b/g, '<span class="tk-num">$1</span>');
  out = out.replace(/\u0001("(?:[^"\\]|\\.)*?")\u0002/g, '<span class="tk-str">$1</span>');
  out = out.replace(/\u0003(\/\/[^\n]*)\u0004/g, '<span class="tk-com">$1</span>');
  return out;
}
