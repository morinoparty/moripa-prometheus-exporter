import type { BaseLayoutProps } from "@/components/layout/shared";

export function baseOptions(): BaseLayoutProps {
    return {
        nav: {
            title: (
                <div className="flex items-center gap-2">
                    <span className="text-lg font-bold">MoripaUtils</span>
                </div>
            ),
            transparentMode: "top",
        },
        themeSwitch: {
            enabled: false,
        },
        modrinthUrl: "https://modrinth.com/plugin/moripa-utils",
        githubUrl: "https://github.com/morinoparty/moripa-utils",
    };
}
