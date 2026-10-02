const active = new WeakMap<HTMLElement, () => void>();
(window as any).testsysCodeEditor = {
  attach(host: HTMLElement, numbers: HTMLElement) {
    this.detach(host);
    const input = host instanceof HTMLTextAreaElement ? host : (host as any).inputElement ?? host.querySelector('textarea') ?? host.shadowRoot?.querySelector('textarea');
    if (!input) return;
    const sync = () => { numbers.textContent = Array.from({length: input.value.split('\n').length}, (_, i) => i + 1).join('\n'); numbers.scrollTop = input.scrollTop; };
    input.spellcheck = false;
    input.addEventListener('input', sync); input.addEventListener('scroll', sync); sync();
    active.set(host, () => { input.removeEventListener('input', sync); input.removeEventListener('scroll', sync); });
  },
  detach(host: HTMLElement) { active.get(host)?.(); active.delete(host); }
};
