// Tiny launcher UI behaviour. Nothing here phones home or launches a binary.
// The buttons are anchors to the real offline client / docs.

const tabs = Array.from(document.querySelectorAll('[data-launcher-tab]'));
const panels = Array.from(document.querySelectorAll('[data-launcher-panel]'));

for (const tab of tabs) {
	tab.addEventListener('click', () => {
		const target = tab.getAttribute('data-launcher-tab');
		for (const t of tabs) t.setAttribute('aria-selected', String(t === tab));
		for (const panel of panels) {
			const active = panel.getAttribute('data-launcher-panel') === target;
			panel.classList.toggle('active', active);
			panel.hidden = !active;
		}
	});
}

const proto = document.getElementById('launcher-proto');
const note = document.getElementById('launcher-proto-note');

if (proto && note) {
	const update = () => {
		note.textContent = proto.value === '764'
			? '1.20.2 wire protocol. Preferred on modern Bungee/Velocity.'
			: '1.18.2 wire protocol. Use when 764 is rejected by the proxy.';
	};
	proto.addEventListener('change', update);
	update();
}
