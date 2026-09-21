import type { BaseLayoutProps } from "@/components/layout/shared";

export function baseOptions(): BaseLayoutProps {
    return {
        nav: {
            title: (
                <div className="flex items-center gap-2">
                    <span className="text-lg font-bold">MoripaPrometheusExporter</span>
                </div>
            ),
            transparentMode: "top",
        },
        themeSwitch: {
            enabled: false,
        },
        modrinthUrl: "https://modrinth.com/plugin/moripa-prometheus-exporter",
        githubUrl: "https://github.com/morinoparty/moripa-prometheus-exporter",
    };
}
