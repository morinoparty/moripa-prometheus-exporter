import { source } from "../../lib/source";

export const revalidate = false;

export async function GET() {
	const pages = source.getPages();

	const records = pages.map((page) => {
		const title = page.data.title;
		const description = page.data.description || "";
		const url = page.url;
		return `- [${title}](${url}): ${description}`;
	});

	const content = `# MoripaPrometheusExporter Documentation

> MoripaPrometheusExporter is a Prometheus exporter plugin for Minecraft (Paper / Velocity) that exposes player count, TPS and other server metrics for Grafana.

## Documentation Pages

${records.join("\n")}

## Full Documentation

For complete documentation content, visit: /llms-full.txt
`;

	return new Response(content, {
		headers: {
			"Content-Type": "text/plain; charset=utf-8",
		},
	});
}
