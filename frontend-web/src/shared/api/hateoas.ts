export type HateoasLink = {
  href: string;
  method?: string;
};

export type HateoasLinkValue = string | HateoasLink | HateoasLink[];

export type HateoasLinks = Record<string, HateoasLinkValue>;

export type ResourceWithLinks = {
  _links?: HateoasLinks;
};

export function linkHref(value: HateoasLinkValue | undefined): string | undefined {
  if (!value) return undefined;
  if (typeof value === "string") return value;
  if (Array.isArray(value)) return linkHref(value[0]);
  return value.href;
}

export function useActions(resource: ResourceWithLinks | undefined) {
  const links = resource?._links ?? {};
  return {
    get: (rel: string) => links[rel],
    can: (rel: string) => rel in links,
    href: (rel: string) => linkHref(links[rel]),
  };
}
