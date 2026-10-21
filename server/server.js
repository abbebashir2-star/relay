import express from "express";
import { WebSocketServer } from "ws";
import { randomBytes } from "crypto";
import { readFileSync } from "fs";
import { fileURLToPath } from "url";
import { dirname, join } from "path";

const PORT = process.env.PORT || 3000;
const here = dirname(fileURLToPath(import.meta.url));
const PAGE = readFileSync(join(here, "public/k.html"), "utf8");

const app = express();
app.use(express.json());

const S = new Map();
const code5 = () => randomBytes(3).toString("hex").toUpperCase().slice(0, 5);
const bcast = (s, msg) => {
  const str = JSON.stringify(msg);
  for (const c of s.clients) if (c.readyState === 1) c.send(str);
};

app.post("/api/session", (_q, r) => {
  const code = code5();
  S.set(code, { token: null, clients: new Set(), timer: null });
  r.json({ code });
});

app.post("/api/session/:code/token", (q, r) => {
  const s = S.get(q.params.code);
  if (!s) return r.status(404).end();
  clearTimeout(s.timer);
  s.token = q.body.token;
  bcast(s, { type: "token", token: s.token, ttl: 30 });
  s.timer = setTimeout(() => { s.token = null; bcast(s, { type: "expired" }); }, 30000);
  r.json({ ok: true });
});

app.get("/k/:code", (q, r) => {
  if (!S.has(q.params.code)) return r.status(404).send("okand kod");
  r.type("html").send(PAGE);
});

const server = app.listen(PORT, () => console.log("relay pa :" + PORT));

new WebSocketServer({ server }).on("connection", (ws, req) => {
  const m = req.url.match(/^\/w\/([A-Z0-9]{5})$/);
  const s = m && S.get(m[1]);
  if (!s) return ws.close();
  s.clients.add(ws);
  ws.on("close", () => s.clients.delete(ws));
  if (s.token) ws.send(JSON.stringify({ type: "token", token: s.token, ttl: 30 }));
});
