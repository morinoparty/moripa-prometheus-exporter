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

	const content = `# MoripaUtils Documentation

> MoripaUtils is morinoparty's utility plugin for Minecraft (Paper / Velocity). Its features include observability (a Prometheus exporter exposing player count, TPS and other server metrics for Grafana) and ticket (an in-game /ticket inquiry UI). This site also hosts the morinoparty mpm repository index.

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
