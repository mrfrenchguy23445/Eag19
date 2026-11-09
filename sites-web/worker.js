// Minimal static-asset Worker for the Sites deployment: every request is
// answered from the uploaded asset tree (index.html, manifest.json, data/*).
export default {
	async fetch(request, env) {
		return env.ASSETS.fetch(request);
	}
};
