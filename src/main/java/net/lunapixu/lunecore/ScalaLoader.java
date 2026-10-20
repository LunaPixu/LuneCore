package net.lunapixu.lunecore;

import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;

public final class ScalaLoader implements PluginLoader {
  @Override
  public void classloader(final PluginClasspathBuilder classpathBuilder) {
    final MavenLibraryResolver resolver = new MavenLibraryResolver();
    resolver.addRepository(
      new RemoteRepository.Builder("paper", "default", "https://repo.papermc.io/repository/maven-public/").build()
    );
    // Paper does not have the full Scala 3 library so it needs to be added to classpath
    resolver.addDependency(new Dependency(new DefaultArtifact("org.scala-lang:scala3-library_3:3.9.0"), null));

    classpathBuilder.addLibrary(resolver);
  }
}
