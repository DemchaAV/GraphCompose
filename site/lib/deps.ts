export const DEPS = {
  maven: `<dependency>
  <groupId>io.github.demchaav</groupId>
  <artifactId>graph-compose</artifactId>
  <version>1.6.6</version>
</dependency>`,
  gradle: `implementation("io.github.demchaav:graph-compose:1.6.6")`,
} as const;

export type DepKind = keyof typeof DEPS;
