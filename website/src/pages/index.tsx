import React from 'react';
import type { HeadFC } from 'gatsby';
import Layout from '../components/Layout';
import Hero from '../components/Hero';
import ProblemSolution from '../components/ProblemSolution';
import Capabilities from '../components/Capabilities';
import Team from '../components/Team';
import Documentation from '../components/Documentation';
import Seo from '../components/Seo';

const Divider = () => <div className="section-divider mx-auto max-w-4xl" />;

const IndexPage: React.FC = () => {
  return (
    <Layout>
      <Hero />
      <Divider />
      <ProblemSolution />
      <Divider />
      <Capabilities />
      <Divider />
      <Team />
      <Divider />
      <Documentation />
    </Layout>
  );
};

export default IndexPage;

export const Head: HeadFC = () => <Seo />;
